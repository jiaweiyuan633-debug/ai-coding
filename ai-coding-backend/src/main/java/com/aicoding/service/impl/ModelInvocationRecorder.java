package com.aicoding.service.impl;

import com.aicoding.mapper.ModelInvocationMapper;
import com.aicoding.model.entity.ModelInvocation;
import dev.langchain4j.model.output.TokenUsage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 模型调用计量：所有 AI 调用统一记录 token/耗时，支撑用量中心与配额
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelInvocationRecorder {

    private final ModelInvocationMapper modelInvocationMapper;

    public void record(Long userId, Long appId, String model, String purpose,
                       TokenUsage usage, long costMs, boolean success) {
        try {
            ModelInvocation invocation = new ModelInvocation();
            invocation.setUserId(userId);
            invocation.setAppId(appId);
            invocation.setModel(model);
            invocation.setPurpose(purpose);
            invocation.setInputTokens(usage == null ? 0 : nullToZero(usage.inputTokenCount()));
            invocation.setOutputTokens(usage == null ? 0 : nullToZero(usage.outputTokenCount()));
            invocation.setCostMs(costMs);
            invocation.setStatus(success ? "success" : "failed");
            modelInvocationMapper.insert(invocation);
        } catch (Exception e) {
            log.warn("[Usage] 计量记录失败: {}", e.getMessage());
        }
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }
}
