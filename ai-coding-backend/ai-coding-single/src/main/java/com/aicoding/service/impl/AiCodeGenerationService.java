package com.aicoding.service.impl;

import com.aicoding.core.workflow.CodeGenWorkflow;
import com.aicoding.core.workflow.WFState;
import com.aicoding.model.entity.App;
import com.aicoding.model.entity.User;
import com.aicoding.model.enums.MessageTypeEnum;
import com.aicoding.service.AppService;
import com.aicoding.service.ChatHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AI 代码生成编排服务（门面模式）：并发锁 + 心跳 + 对话记忆 + 工作流执行
 * 工作流细节见 {@link CodeGenWorkflow}（LangGraph4j：路由→生成→质检→修复→快照）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiCodeGenerationService {

    private static final long HEARTBEAT_SECONDS = 15;
    private static final int HISTORY_WINDOW = 6;

    private final CodeGenWorkflow codeGenWorkflow;
    private final AppService appService;
    private final ChatHistoryService chatHistoryService;
    private final com.aicoding.service.CoverService coverService;

    /**
     * 应用级生成锁：同一应用同时只允许一个生成任务
     */
    private final ConcurrentHashMap<Long, Boolean> generatingLock = new ConcurrentHashMap<>();

    private final ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();

    public SseEmitter chatToGenerate(long appId, String userMessage, User user) {
        App app = appService.getAppById(appId);
        appService.checkAppOwner(app, user);
        if (StringUtils.isBlank(userMessage)) {
            throw new com.aicoding.common.BusinessException(com.aicoding.common.ErrorCode.PARAMS_BLANK, "请输入你的需求");
        }
        Boolean existing = generatingLock.putIfAbsent(appId, Boolean.TRUE);
        if (existing != null) {
            throw new com.aicoding.common.BusinessException(com.aicoding.common.ErrorCode.TOO_MANY_REQUEST,
                    "该应用正在生成中，请稍候");
        }

        SseEmitter emitter = new SseEmitter(0L);
        AtomicBoolean lockReleased = new AtomicBoolean(false);
        emitter.onCompletion(() -> release(appId, lockReleased));
        emitter.onTimeout(() -> release(appId, lockReleased));

        // 对话记忆：先取历史，再落库本次用户消息
        List<com.aicoding.model.dto.chat.ChatHistoryVO> history =
                chatHistoryService.listRecentByApp(appId, HISTORY_WINDOW);
        chatHistoryService.addChatMessage(appId, user.getId(), userMessage, MessageTypeEnum.USER);

        Thread.startVirtualThread(() -> {
            ScheduledFuture<?> heartbeat = heartbeatScheduler.scheduleAtFixedRate(() -> {
                try {
                    emitter.send(SseEmitter.event().name("ping").data("1"));
                } catch (Exception ignore) {
                    // 连接已断开，等待完成/超时清理
                }
            }, HEARTBEAT_SECONDS, HEARTBEAT_SECONDS, TimeUnit.SECONDS);
            try {
                CodeGenWorkflow.RunContext ctx = new CodeGenWorkflow.RunContext(
                        appId, user.getId(), userMessage, StringUtils.isBlank(app.getCodeGenType()), emitter);
                WFState finalState = codeGenWorkflow.run(ctx, app.getCodeGenType());

                // 收尾：应用元信息回写 + AI 回执入库
                if (finalState != null && finalState.genType() != null
                        && !finalState.genType().equals(app.getCodeGenType())) {
                    app.setCodeGenType(finalState.genType());
                }
                if ("未命名应用".equals(app.getAppName())) {
                    app.setAppName(abbreviate(app.getInitPrompt(), 16));
                }
                if (app.getCover() == null || app.getCover().isBlank()) {
                    coverService.generateCover(appId, app.getAppName());
                    app.setCover("/api/cover/" + appId);
                }
                appService.updateGeneratedMeta(app);
                chatHistoryService.addChatMessage(appId, user.getId(),
                        "已完成生成并保存为 v" + (finalState == null ? 1 : safeVersion(finalState))
                                + "，点击预览查看效果，可继续对话修改。", MessageTypeEnum.AI);
                emitter.complete();
            } catch (Exception e) {
                log.error("[AI] 生成异常: appId={}", appId, e);
                try {
                    emitter.send(SseEmitter.event().name("error")
                            .data(Map.of("message", e.getMessage() == null ? "生成失败" : e.getMessage())));
                } catch (Exception ignore) {
                    // ignore
                }
                emitter.complete();
            } finally {
                heartbeat.cancel(false);
            }
        });
        return emitter;
    }

    private int safeVersion(WFState state) {
        Object v = state.value("version");
        return v == null ? 1 : Integer.parseInt(v.toString());
    }

    private void release(long appId, AtomicBoolean released) {
        if (released.compareAndSet(false, true)) {
            generatingLock.remove(appId);
        }
    }

    private String abbreviate(String s, int len) {
        String clean = s == null ? "" : s.replaceAll("\\s+", " ").trim();
        return clean.length() > len ? clean.substring(0, len) : clean;
    }
}
