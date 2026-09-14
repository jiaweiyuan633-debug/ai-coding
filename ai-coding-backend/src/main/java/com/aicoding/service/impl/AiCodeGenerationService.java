package com.aicoding.service.impl;

import cn.hutool.json.JSONUtil;
import com.aicoding.common.BusinessException;
import com.aicoding.common.ErrorCode;
import com.aicoding.config.AiProperties;
import com.aicoding.core.ai.AiModelManager;
import com.aicoding.core.ai.AiPrompts;
import com.aicoding.core.ai.CodeGenRouter;
import com.aicoding.core.ai.CodeStreamParser;
import com.aicoding.core.ai.WorkspaceUtils;
import com.aicoding.core.ai.tools.MultiFileAiService;
import com.aicoding.core.ai.tools.WriteFileTool;
import com.aicoding.model.dto.chat.ChatHistoryVO;
import com.aicoding.model.entity.App;
import com.aicoding.model.entity.User;
import com.aicoding.model.enums.CodeGenTypeEnum;
import com.aicoding.model.enums.MessageTypeEnum;
import com.aicoding.service.AppService;
import com.aicoding.service.ChatHistoryService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
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
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AI 代码生成编排服务（门面模式）：
 * 智能路由 -> 流式生成（单文件直出代码 / 多文件走工具调用）-> 落盘 -> 对话记忆
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiCodeGenerationService {

    private static final long HEARTBEAT_SECONDS = 15;
    private static final int HISTORY_WINDOW = 6;

    private final AiModelManager aiModelManager;
    private final AiProperties aiProperties;
    private final CodeGenRouter codeGenRouter;
    private final WorkspaceUtils workspaceUtils;
    private final AppService appService;
    private final ChatHistoryService chatHistoryService;

    /**
     * 应用级生成锁：同一应用同时只允许一个生成任务
     */
    private final ConcurrentHashMap<Long, Boolean> generatingLock = new ConcurrentHashMap<>();

    private final ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();

    public SseEmitter chatToGenerate(long appId, String userMessage, User user) {
        App app = appService.getAppById(appId);
        appService.checkAppOwner(app, user);
        if (StringUtils.isBlank(userMessage)) {
            throw new BusinessException(ErrorCode.PARAMS_BLANK, "请输入你的需求");
        }
        Boolean existing = generatingLock.putIfAbsent(appId, Boolean.TRUE);
        if (existing != null) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, "该应用正在生成中，请稍候");
        }

        SseEmitter emitter = new SseEmitter(0L);
        AtomicBoolean lockReleased = new AtomicBoolean(false);
        emitter.onCompletion(() -> release(appId, lockReleased));
        emitter.onTimeout(() -> release(appId, lockReleased));

        Thread.startVirtualThread(() -> {
            ScheduledFuture<?> heartbeat = heartbeatScheduler.scheduleAtFixedRate(() -> {
                try {
                    emitter.send(SseEmitter.event().name("ping").data("1"));
                } catch (Exception ignore) {
                    // 连接已断开，等待超时/完成清理
                }
            }, HEARTBEAT_SECONDS, HEARTBEAT_SECONDS, TimeUnit.SECONDS);
            try {
                doGenerate(emitter, app, user, userMessage);
            } catch (Exception e) {
                log.error("[AI] 生成异常: appId={}", appId, e);
                sendEvent(emitter, "error", Map.of("message", e.getMessage() == null ? "生成失败" : e.getMessage()));
                emitter.complete();
            } finally {
                heartbeat.cancel(false);
            }
        });
        return emitter;
    }

    private void doGenerate(SseEmitter emitter, App app, User user, String userMessage) {
        long appId = app.getId();
        boolean firstGeneration = StringUtils.isBlank(app.getCodeGenType());
        CodeGenTypeEnum genType;
        if (firstGeneration) {
            genType = codeGenRouter.route(app.getInitPrompt());
            app.setCodeGenType(genType.getValue());
            sendEvent(emitter, "router", Map.of("codeGenType", genType.getValue(), "text", genType.getText()));
            log.info("[AI] 路由决策: appId={} -> {}", appId, genType.getValue());
        } else {
            genType = CodeGenTypeEnum.fromValue(app.getCodeGenType());
            if (genType == null) {
                genType = CodeGenTypeEnum.HTML_SINGLE;
            }
        }

        List<ChatHistoryVO> history = chatHistoryService.listRecentByApp(appId, HISTORY_WINDOW);
        chatHistoryService.addChatMessage(appId, user.getId(), userMessage, MessageTypeEnum.USER);

        switch (genType) {
            case HTML_SINGLE -> generateSingleFile(emitter, app, user, userMessage, history, firstGeneration);
            case HTML_MULTI_FILE -> generateMultiFile(emitter, app, user, userMessage, firstGeneration);
            case VUE_PROJECT -> throw new BusinessException(ErrorCode.AI_GENERATION_ERROR,
                    "Vue 工程生成即将上线，请先尝试网页类需求（如：制作一个xx展示页/工具页/小游戏）");
        }
    }

    /**
     * 单文件模式：代码增量实时透传（打字机 + 实时预览）
     */
    private void generateSingleFile(SseEmitter emitter, App app, User user, String userMessage,
                                    List<ChatHistoryVO> history, boolean firstGeneration) {
        long appId = app.getId();
        CodeStreamParser parser = new CodeStreamParser();

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(AiPrompts.HTML_SINGLE_SYSTEM));
        if (!firstGeneration) {
            // 对话修改：携带当前完整代码
            String currentCode = workspaceUtils.readFile(appId, "index.html");
            if (currentCode != null) {
                messages.add(UserMessage.from(AiPrompts.MODIFY_PROMPT_TEMPLATE.formatted(currentCode, userMessage)));
            } else {
                messages.add(UserMessage.from(userMessage));
            }
        } else {
            messages.add(UserMessage.from(userMessage));
        }
        ChatRequest request = ChatRequest.builder().messages(messages).build();

        StreamingChatModel model = aiModelManager.streamingChatModel();
        model.chat(request, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String token) {
                String cleaned = parser.onDelta(token);
                if (!cleaned.isEmpty()) {
                    sendEvent(emitter, "delta", Map.of("text", cleaned));
                }
            }

            @Override
            public void onCompleteResponse(ChatResponse response) {
                String code = parser.complete();
                if (StringUtils.isBlank(code) || !code.contains("<")) {
                    sendEvent(emitter, "error", Map.of("message", "AI 未返回有效代码，请重试"));
                    emitter.complete();
                    return;
                }
                workspaceUtils.writeFile(appId, "index.html", code);
                finishGeneration(emitter, app, user, 1);
            }

            @Override
            public void onError(Throwable error) {
                log.error("[AI] 单文件生成失败: appId={}", appId, error);
                sendEvent(emitter, "error", Map.of("message", "生成失败：" + error.getMessage()));
                emitter.complete();
            }
        });
    }

    /**
     * 多文件模式：AiServices + writeFile 工具调用
     */
    private void generateMultiFile(SseEmitter emitter, App app, User user, String userMessage, boolean firstGeneration) {
        long appId = app.getId();
        WriteFileTool tool = new WriteFileTool(workspaceUtils.appDir(appId), (path, content) ->
                sendEvent(emitter, "tool", Map.of("path", path, "size", content.length())));

        MultiFileAiService service = AiServices.builder(MultiFileAiService.class)
                .streamingChatModel(aiModelManager.streamingChatModel())
                .systemMessage(AiPrompts.HTML_MULTI_FILE_SYSTEM)
                .tools(tool)
                .build();

        String message = userMessage;
        if (!firstGeneration) {
            List<String> existingFiles = workspaceUtils.listFiles(appId);
            message = """
                    当前应用已包含以下文件：%s
                    修改要求：%s
                    请重新写入所有需要修改的文件（必须保持 index.html 为入口且引用关系正确）。
                    """.formatted(String.join("、", existingFiles), userMessage);
        }

        TokenStream tokenStream = service.generate(message);
        tokenStream
                .onPartialResponse(token -> {
                    if (StringUtils.isNotBlank(token)) {
                        sendEvent(emitter, "delta", Map.of("text", token));
                    }
                })
                .onCompleteResponse(response -> finishGeneration(emitter, app, user, Math.max(tool.getFileCount(), 1)))
                .onError(error -> {
                    log.error("[AI] 多文件生成失败: appId={}", appId, error);
                    sendEvent(emitter, "error", Map.of("message", "生成失败：" + error.getMessage()));
                    emitter.complete();
                })
                .start();
    }

    /**
     * 生成完成统一收尾：自动取名 + AI 回执入库 + done 事件
     */
    private void finishGeneration(SseEmitter emitter, App app, User user, int fileCount) {
        long appId = app.getId();
        List<String> files = workspaceUtils.listFiles(appId);

        // 首次生成：自动命名（取需求前 16 字）
        if ("未命名应用".equals(app.getAppName()) || StringUtils.isBlank(app.getAppName())) {
            app.setAppName(abbreviate(app.getInitPrompt(), 16));
        }
        app.setCover(app.getCover() != null ? app.getCover() : "");
        appService.updateGeneratedMeta(app);

        String summary = fileCount > 0
                ? "已完成生成（共 " + files.size() + " 个文件），点击预览查看效果，可继续对话修改。"
                : "生成完成。";
        chatHistoryService.addChatMessage(appId, user.getId(), summary, MessageTypeEnum.AI);

        Map<String, Object> done = new HashMap<>();
        done.put("files", files);
        done.put("previewUrl", "/api/preview/" + appId + "/index.html");
        sendEvent(emitter, "done", done);
        emitter.complete();
        log.info("[AI] 生成完成: appId={}, files={}", appId, files.size());
    }

    private void sendEvent(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(JSONUtil.toJsonStr(data)));
        } catch (Exception e) {
            log.warn("[AI] SSE 推送失败（连接可能已断开）: {}", e.getMessage());
        }
    }

    private void release(long appId, AtomicBoolean released) {
        if (released.compareAndSet(false, true)) {
            generatingLock.remove(appId);
        }
    }

    private String abbreviate(String s, int len) {
        String clean = s.replaceAll("\\s+", " ").trim();
        return clean.length() > len ? clean.substring(0, len) : clean;
    }
}
