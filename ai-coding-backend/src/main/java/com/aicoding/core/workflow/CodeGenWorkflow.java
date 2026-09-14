package com.aicoding.core.workflow;

import cn.hutool.json.JSONUtil;
import com.aicoding.core.ai.AiModelManager;
import com.aicoding.core.ai.AiPrompts;
import com.aicoding.core.ai.CodeGenRouter;
import com.aicoding.core.ai.CodeStreamParser;
import com.aicoding.core.ai.WorkspaceUtils;
import com.aicoding.core.ai.tools.MultiFileAiService;
import com.aicoding.core.ai.tools.WriteFileTool;
import com.aicoding.model.enums.CodeGenTypeEnum;
import com.aicoding.service.AppVersionService;
import com.aicoding.service.impl.ModelInvocationRecorder;
import com.aicoding.service.impl.WorkflowAuditService;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncEdgeAction;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 代码生成工作流（LangGraph4j 编排，透明工作流核心）：
 * START → route → generate → validate -pass→ finalize → END
 *                             └fail→ repair → validate（最多 3 轮）
 * 每个节点状态通过 SSE 实时推送 + workflow_run 落库。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CodeGenWorkflow {

    private static final String START = "__START__";
    private static final String END = "__END__";

    public static final String NODE_ROUTE = "route";
    public static final String NODE_GENERATE = "generate";
    public static final String NODE_VALIDATE = "validate";
    public static final String NODE_REPAIR = "repair";
    public static final String NODE_FINALIZE = "finalize";

    private static final int MAX_REPAIR_ROUNDS = 3;

    /**
     * 当前执行上下文（SSE emitter 不可序列化，不进 LangGraph 状态）
     */
    private static final ThreadLocal<RunContext> CURRENT_CTX = new ThreadLocal<>();

    private final AiModelManager aiModelManager;
    private final CodeGenRouter codeGenRouter;
    private final QualityGate qualityGate;
    private final WorkspaceUtils workspaceUtils;
    private final WorkflowAuditService workflowAuditService;
    private final ModelInvocationRecorder invocationRecorder;
    private final AppVersionService appVersionService;

    /**
     * 工作流执行上下文（LangGraph 状态之外的粘合对象）
     */
    public record RunContext(long appId, long userId, String userMessage, boolean firstGeneration,
                             SseEmitter emitter) {
    }

    /**
     * 构建并同步执行工作流，返回最终状态（含 genType 等决策结果）
     */
    public WFState run(RunContext ctx, String initialGenType) throws Exception {
        StateGraph<WFState> graph = new StateGraph<>(WFState.mapFactory(), WFState::new)
                .addNode(NODE_ROUTE, AsyncNodeAction.node_async(this::routeNode))
                .addNode(NODE_GENERATE, AsyncNodeAction.node_async(this::generateNode))
                .addNode(NODE_VALIDATE, AsyncNodeAction.node_async(this::validateNode))
                .addNode(NODE_REPAIR, AsyncNodeAction.node_async(this::repairNode))
                .addNode(NODE_FINALIZE, AsyncNodeAction.node_async(this::finalizeNode))
                .addEdge(START, NODE_ROUTE)
                .addEdge(NODE_ROUTE, NODE_GENERATE)
                .addEdge(NODE_GENERATE, NODE_VALIDATE)
                .addConditionalEdges(NODE_VALIDATE, AsyncEdgeAction.edge_async(this::afterValidate),
                        Map.of(NODE_REPAIR, NODE_REPAIR, NODE_FINALIZE, NODE_FINALIZE))
                .addEdge(NODE_REPAIR, NODE_VALIDATE)
                .addEdge(NODE_FINALIZE, END);

        CompiledGraph<WFState> compiled = graph.compile();
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("appId", ctx.appId());
        inputs.put("userId", ctx.userId());
        inputs.put("userMessage", ctx.userMessage());
        inputs.put("firstGeneration", ctx.firstGeneration());
        if (initialGenType != null) {
            inputs.put("genType", initialGenType);
        }
        CURRENT_CTX.set(ctx);
        try {
            return compiled.invoke(inputs, org.bsc.langgraph4j.RunnableConfig.builder().build()).orElse(null);
        } finally {
            CURRENT_CTX.remove();
        }
    }

    /**
     * 节点：智能路由（仅首次生成执行决策）
     */
    private Map<String, Object> routeNode(WFState state) throws Exception {
        long start = System.currentTimeMillis();
        nodeEvent(state, NODE_ROUTE, "running", "分析需求，选择生成类型…");
        Map<String, Object> updates = new HashMap<>();
        if (!Boolean.TRUE.equals(state.firstGeneration())) {
            nodeEvent(state, NODE_ROUTE, "success", "沿用既有生成类型 " + state.genType());
            workflowAuditService.record(state.appId(), runId(state), NODE_ROUTE, "success",
                    Map.of("genType", state.genType(), "reused", true), System.currentTimeMillis() - start);
            return updates;
        }
        CodeGenTypeEnum decided = codeGenRouter.route(workspaceUtils.appDir(state.appId()) != null
                ? state.userMessage() : state.userMessage());
        updates.put("genType", decided.getValue());
        nodeEvent(state, NODE_ROUTE, "success", "生成类型：" + decided.getText());
        workflowAuditService.record(state.appId(), runId(state), NODE_ROUTE, "success",
                Map.of("genType", decided.getValue()), System.currentTimeMillis() - start);
        return updates;
    }

    /**
     * 节点：代码生成（单文件直出代码流 / 多文件走工具调用）
     */
    private Map<String, Object> generateNode(WFState state) throws Exception {
        long start = System.currentTimeMillis();
        boolean first = Boolean.TRUE.equals(state.firstGeneration());
        nodeEvent(state, NODE_GENERATE, "running", "AI 正在生成代码…");
        String purpose = "generate";

        if (CodeGenTypeEnum.VUE_PROJECT.getValue().equals(state.genType())) {
            throw new IllegalStateException("Vue 工程生成即将上线，请先尝试网页类需求");
        }

        if (CodeGenTypeEnum.HTML_MULTI_FILE.getValue().equals(state.genType())) {
            generateMultiFileStreaming(state, first, purpose, start);
            return Map.of();
        }
        String code = generateSingleFileStreaming(state, first, purpose, start);
        return Map.of("code", code);
    }

    /**
     * 节点：质量门禁
     */
    private Map<String, Object> validateNode(WFState state) {
        long start = System.currentTimeMillis();
        nodeEvent(state, NODE_VALIDATE, "running", "静态校验生成产物…");
        List<String> issues;
        if (CodeGenTypeEnum.HTML_MULTI_FILE.getValue().equals(state.genType())) {
            Map<String, String> files = readWorkspaceFiles(state.appId());
            issues = qualityGate.validateMultiFile(files);
        } else {
            issues = qualityGate.validateSingleFile(state.code());
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put("issues", issues);
        String status = issues.isEmpty() ? "success" : "failed";
        nodeEvent(state, NODE_VALIDATE, status, issues.isEmpty()
                ? "校验通过"
                : "发现 " + issues.size() + " 个问题：" + String.join("；", issues));
        workflowAuditService.record(state.appId(), runId(state), NODE_VALIDATE, status, issues,
                System.currentTimeMillis() - start);
        return updates;
    }

    /**
     * 校验后路由：通过 → finalize；失败且有修复次数 → repair；否则带问题放行
     */
    private String afterValidate(WFState state) {
        List<String> issues = state.issues();
        boolean pass = issues == null || issues.isEmpty();
        if (pass) {
            return NODE_FINALIZE;
        }
        if (state.round() < MAX_REPAIR_ROUNDS) {
            return NODE_REPAIR;
        }
        log.warn("[Workflow] 修复 {} 轮后仍有问题，放行: appId={}", MAX_REPAIR_ROUNDS, state.appId());
        return NODE_FINALIZE;
    }

    /**
     * 节点：AI 自动修复
     */
    private Map<String, Object> repairNode(WFState state) throws Exception {
        long start = System.currentTimeMillis();
        int round = state.round() + 1;
        nodeEvent(state, NODE_REPAIR, "running", "第 " + round + " 轮自动修复（共 " + issuesText(state) + "）…");
        String purpose = "repair";

        if (CodeGenTypeEnum.HTML_MULTI_FILE.getValue().equals(state.genType())) {
            String repairMessage = """
                    当前应用已包含以下文件：%s
                    质量校验发现以下问题，请修复并重新写入相关文件：
                    %s
                    """.formatted(String.join("、", workspaceUtils.listFiles(state.appId())),
                    String.join("\n", state.issues()));
            runMultiFileService(state, purpose, repairMessage);
        } else {
            String repairPrompt = """
                    以下 HTML 代码未通过质量校验：
                    ```html
                    %s
                    ```
                    问题清单：
                    %s
                    请输出修复后的完整代码（单文件 HTML，只输出代码本身）。
                    """.formatted(state.code(), String.join("\n", state.issues()));
            String fixed = generateSingleFileStreaming(state, true, purpose, start, repairPrompt);
            Map<String, Object> updates = new HashMap<>();
            updates.put("round", round);
            updates.put("code", fixed);
            nodeEvent(state, NODE_REPAIR, "success", "第 " + round + " 轮修复完成");
            workflowAuditService.record(state.appId(), runId(state), NODE_REPAIR, "success",
                    Map.of("round", round), System.currentTimeMillis() - start);
            return updates;
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put("round", round);
        nodeEvent(state, NODE_REPAIR, "success", "第 " + round + " 轮修复完成");
        workflowAuditService.record(state.appId(), runId(state), NODE_REPAIR, "success",
                Map.of("round", round), System.currentTimeMillis() - start);
        return updates;
    }

    /**
     * 节点：收尾（写盘 + 版本快照 + done 事件）
     */
    private Map<String, Object> finalizeNode(WFState state) throws Exception {
        long start = System.currentTimeMillis();
        nodeEvent(state, NODE_FINALIZE, "running", "保存产物、生成版本快照…");

        // 单文件：落盘 index.html；多文件：已由工具直接写入
        if (CodeGenTypeEnum.HTML_SINGLE.getValue().equals(state.genType()) && state.code() != null) {
            workspaceUtils.writeFile(state.appId(), "index.html", state.code());
        }
        List<String> files = workspaceUtils.listFiles(state.appId());
        String message = state.userMessage();

        Map<String, Object> payload = new HashMap<>();
        payload.put("files", files);
        payload.put("genType", state.genType());
        payload.put("rounds", state.round());
        workflowAuditService.record(state.appId(), runId(state), NODE_FINALIZE, "success", payload,
                System.currentTimeMillis() - start);

        nodeEvent(state, NODE_FINALIZE, "success", "完成（" + files.size() + " 个文件，修复 " + state.round() + " 轮）");

        // 版本时光机：本次产物自动快照
        com.aicoding.model.entity.AppVersion version =
                appVersionService.createSnapshot(state.appId(), state.userId(), abbreviate(message, 60), state.genType());

        Map<String, Object> done = new HashMap<>();
        done.put("files", files);
        done.put("previewUrl", "/api/preview/" + state.appId() + "/index.html");
        done.put("version", version.getVersion());
        sendSse(state, "done", done);
        return Map.of();
    }

    // ==================== 生成实现 ====================

    /**
     * 单文件流式生成，返回清洗后的完整代码
     */
    private String generateSingleFileStreaming(WFState state, boolean first, String purpose,
                                               long start) throws Exception {
        String prompt;
        if (first) {
            prompt = state.userMessage();
        } else {
            String currentCode = workspaceUtils.readFile(state.appId(), "index.html");
            prompt = currentCode != null
                    ? AiPrompts.MODIFY_PROMPT_TEMPLATE.formatted(currentCode, state.userMessage())
                    : state.userMessage();
        }
        return generateSingleFileStreaming(state, true, purpose, start, prompt);
    }

    /**
     * 单文件流式生成（指定提示词），返回清洗后的完整代码
     */
    private String generateSingleFileStreaming(WFState state, boolean withSystem, String purpose,
                                               long start, String prompt) throws Exception {
        CodeStreamParser parser = new CodeStreamParser();
        List<dev.langchain4j.data.message.ChatMessage> messages = new ArrayList<>();
        if (withSystem) {
            messages.add(SystemMessage.from(AiPrompts.HTML_SINGLE_SYSTEM));
        }
        messages.add(UserMessage.from(prompt));
        ChatRequest request = ChatRequest.builder().messages(messages).build();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<ChatResponse> finalResponse = new AtomicReference<>();
        StreamingChatModel model = aiModelManager.streamingModelFor(purpose);
        model.chat(request, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String token) {
                String cleaned = parser.onDelta(token);
                if (!cleaned.isEmpty()) {
                    sendSse(state, "delta", Map.of("text", cleaned));
                }
            }

            @Override
            public void onCompleteResponse(ChatResponse response) {
                finalResponse.set(response);
                latch.countDown();
            }

            @Override
            public void onError(Throwable error) {
                latch.countDown();
                sendSse(state, "error", Map.of("message", "生成失败：" + error.getMessage()));
            }
        });
        if (!latch.await(5, TimeUnit.MINUTES)) {
            throw new IllegalStateException("生成超时");
        }
        invocationRecorder.record(state.userId(), state.appId(), "streaming-model", purpose,
                finalResponse.get() == null ? null : finalResponse.get().tokenUsage(),
                System.currentTimeMillis() - start, finalResponse.get() != null);
        return parser.complete();
    }

    /**
     * 多文件流式生成（AiServices + writeFile 工具）
     */
    private void generateMultiFileStreaming(WFState state, boolean first, String purpose, long start) throws Exception {
        String message;
        if (first) {
            message = state.userMessage();
        } else {
            message = """
                    当前应用已包含以下文件：%s
                    修改要求：%s
                    请重新写入所有需要修改的文件（必须保持 index.html 为入口且引用关系正确）。
                    """.formatted(String.join("、", workspaceUtils.listFiles(state.appId())), state.userMessage());
        }
        runMultiFileService(state, purpose, message);
    }

    private void runMultiFileService(WFState state, String purpose, String message) throws Exception {
        long start = System.currentTimeMillis();
        WriteFileTool tool = new WriteFileTool(workspaceUtils.appDir(state.appId()), (path, content) ->
                sendSse(state, "tool", Map.of("path", path, "size", content.length())));

        MultiFileAiService service = AiServices.builder(MultiFileAiService.class)
                .streamingChatModel(aiModelManager.streamingModelFor(purpose))
                .systemMessage(AiPrompts.HTML_MULTI_FILE_SYSTEM)
                .tools(tool)
                .build();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<ChatResponse> finalResponse = new AtomicReference<>();
        TokenStream tokenStream = service.generate(message);
        tokenStream
                .onPartialResponse(token -> {
                    if (StringUtils.isNotBlank(token)) {
                        sendSse(state, "delta", Map.of("text", token));
                    }
                })
                .onCompleteResponse(response -> {
                    finalResponse.set(response);
                    latch.countDown();
                })
                .onError(error -> {
                    latch.countDown();
                    sendSse(state, "error", Map.of("message", "生成失败：" + error.getMessage()));
                })
                .start();
        if (!latch.await(5, TimeUnit.MINUTES)) {
            throw new IllegalStateException("生成超时");
        }
        invocationRecorder.record(state.userId(), state.appId(), "streaming-model", purpose,
                finalResponse.get() == null ? null : finalResponse.get().tokenUsage(),
                System.currentTimeMillis() - start, finalResponse.get() != null);
    }

    // ==================== 工具方法 ====================

    private Map<String, String> readWorkspaceFiles(long appId) {
        Map<String, String> files = new HashMap<>();
        for (String path : workspaceUtils.listFiles(appId)) {
            String content = workspaceUtils.readFile(appId, path);
            if (content != null) {
                files.put(path, content);
            }
        }
        return files;
    }

    private String issuesText(WFState state) {
        List<String> issues = state.issues();
        return issues == null ? "无" : issues.size() + " 个问题";
    }

    private void nodeEvent(WFState state, String node, String status, String summary) {
        Map<String, Object> event = new HashMap<>();
        event.put("node", node);
        event.put("status", status);
        event.put("summary", summary);
        sendSse(state, "workflow", event);
    }

    private void sendSse(WFState state, String event, Object data) {
        RunContext ctx = CURRENT_CTX.get();
        SseEmitter emitter = ctx == null ? null : ctx.emitter();
        if (emitter == null) {
            return;
        }
        try {
            emitter.send(SseEmitter.event().name(event).data(JSONUtil.toJsonStr(data)));
        } catch (Exception e) {
            log.debug("[Workflow] SSE 推送失败（连接可能已断开）: {}", e.getMessage());
        }
    }

    private String runId(WFState state) {
        return state.appId() + "-" + System.currentTimeMillis() / 1000;
    }

    private String abbreviate(String s, int len) {
        String clean = s == null ? "" : s.replaceAll("\\s+", " ").trim();
        return clean.length() > len ? clean.substring(0, len) : clean;
    }
}
