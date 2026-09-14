package com.aicoding.service.impl;

import cn.hutool.json.JSONUtil;
import com.aicoding.api.AppRpcService;
import com.aicoding.api.dto.AppDTO;
import com.aicoding.api.dto.AppMetaDTO;
import com.aicoding.common.BusinessException;
import com.aicoding.common.ErrorCode;
import com.aicoding.core.workflow.CodeGenWorkflow;
import com.aicoding.core.workflow.WFState;
import com.aicoding.model.dto.chat.ChatHistoryVO;
import com.aicoding.model.entity.User;
import com.aicoding.model.enums.MessageTypeEnum;
import com.aicoding.service.ChatHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
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
 * AI 代码生成编排服务（微服务版）：应用归属与元信息回写走 Dubbo RPC（app-service）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiCodeGenerationService {

    private static final long HEARTBEAT_SECONDS = 15;
    private static final int HISTORY_WINDOW = 6;

    private final CodeGenWorkflow codeGenWorkflow;
    private final ChatHistoryService chatHistoryService;

    @DubboReference(check = false)
    private AppRpcService appRpcService;

    private final ConcurrentHashMap<Long, Boolean> generatingLock = new ConcurrentHashMap<>();

    private final ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();

    public SseEmitter chatToGenerate(long appId, String userMessage, User user) {
        AppDTO app = appRpcService.getApp(appId);
        if (app == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        }
        boolean admin = "admin".equals(user.getUserRole());
        if (app.getUserId() != user.getId() && !admin) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作该应用");
        }
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

        List<ChatHistoryVO> history = chatHistoryService.listRecentByApp(appId, HISTORY_WINDOW);
        chatHistoryService.addChatMessage(appId, user.getId(), userMessage, MessageTypeEnum.USER);

        Thread.startVirtualThread(() -> {
            ScheduledFuture<?> heartbeat = heartbeatScheduler.scheduleAtFixedRate(() -> {
                try {
                    emitter.send(SseEmitter.event().name("ping").data("1"));
                } catch (Exception ignore) {
                }
            }, HEARTBEAT_SECONDS, HEARTBEAT_SECONDS, TimeUnit.SECONDS);
            try {
                CodeGenWorkflow.RunContext ctx = new CodeGenWorkflow.RunContext(
                        appId, user.getId(), userMessage, StringUtils.isBlank(app.getCodeGenType()), emitter);
                WFState finalState = codeGenWorkflow.run(ctx, app.getCodeGenType());

                // 元信息回写（RPC 到 app-service；封面缺失由 app-service 生成）
                AppMetaDTO meta = new AppMetaDTO();
                meta.setId(appId);
                if ("未命名应用".equals(app.getAppName())) {
                    meta.setAppName(abbreviate(app.getInitPrompt(), 16));
                }
                if (finalState != null && finalState.genType() != null) {
                    meta.setCodeGenType(finalState.genType());
                }
                appRpcService.saveGeneratedMeta(meta);

                chatHistoryService.addChatMessage(appId, user.getId(),
                        "已完成生成并保存为 v" + (finalState == null ? 1 : safeVersion(finalState))
                                + "，点击预览查看效果，可继续对话修改。", MessageTypeEnum.AI);
                emitter.complete();
            } catch (Exception e) {
                log.error("[AI] 生成异常: appId={}", appId, e);
                try {
                    emitter.send(SseEmitter.event().name("error")
                            .data(JSONUtil.toJsonStr(Map.of("message",
                                    e.getMessage() == null ? "生成失败" : e.getMessage()))));
                } catch (Exception ignore) {
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
