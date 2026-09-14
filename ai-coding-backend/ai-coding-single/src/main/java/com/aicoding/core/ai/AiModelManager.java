package com.aicoding.core.ai;

import com.aicoding.common.BusinessException;
import com.aicoding.common.ErrorCode;
import com.aicoding.config.AiProperties;
import com.aicoding.core.ai.mock.MockChatModel;
import com.aicoding.core.ai.mock.MockStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 模型管理器（策略模式）：统一产出 ChatModel / StreamingChatModel。
 * Mock 模式用于无 API Key 时开发联调；正式模式对接 DeepSeek（OpenAI 兼容协议）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiModelManager {

    private final AiProperties aiProperties;

    private volatile ChatModel defaultChatModel;
    private volatile StreamingChatModel defaultStreamingChatModel;

    /**
     * 多模型路由：按模型名缓存实例（策略模式）
     */
    private final ConcurrentHashMap<String, ChatModel> chatModelCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, StreamingChatModel> streamingModelCache = new ConcurrentHashMap<>();

    public boolean isMock() {
        return aiProperties.isMockEnabled() || StringUtils.isBlank(aiProperties.getDeepseek().getApiKey());
    }

    /**
     * 按用途获取非流式模型（多模型路由入口）
     *
     * @param purpose 用途：router / generate / repair
     */
    public ChatModel chatModelFor(String purpose) {
        if (isMock()) {
            return chatModel();
        }
        String modelName = resolveModelName(purpose);
        return chatModelCache.computeIfAbsent(modelName, name ->
                OpenAiChatModel.builder()
                        .baseUrl(aiProperties.getDeepseek().getBaseUrl())
                        .apiKey(aiProperties.getDeepseek().getApiKey())
                        .modelName(name)
                        .timeout(java.time.Duration.ofSeconds(60))
                        .build());
    }

    /**
     * 按用途获取流式模型（多模型路由入口）
     */
    public StreamingChatModel streamingModelFor(String purpose) {
        if (isMock()) {
            return streamingChatModel();
        }
        String modelName = resolveModelName(purpose);
        return streamingModelCache.computeIfAbsent(modelName, name ->
                OpenAiStreamingChatModel.builder()
                        .baseUrl(aiProperties.getDeepseek().getBaseUrl())
                        .apiKey(aiProperties.getDeepseek().getApiKey())
                        .modelName(name)
                        .timeout(java.time.Duration.ofSeconds(aiProperties.getTimeoutSeconds()))
                        .build());
    }

    private String resolveModelName(String purpose) {
        String name = aiProperties.getPurposeModels().getOrDefault(purpose, aiProperties.getDeepseek().getChatModel());
        return StringUtils.defaultIfBlank(name, aiProperties.getDeepseek().getChatModel());
    }

    /**
     * 非流式模型（默认，用于路由决策等短请求）
     */
    public ChatModel chatModel() {
        if (defaultChatModel == null) {
            synchronized (this) {
                if (defaultChatModel == null) {
                    if (isMock()) {
                        log.info("[AI] 使用 Mock ChatModel");
                        defaultChatModel = new MockChatModel();
                    } else {
                        AiProperties.DeepSeek ds = aiProperties.getDeepseek();
                        defaultChatModel = OpenAiChatModel.builder()
                                .baseUrl(ds.getBaseUrl())
                                .apiKey(ds.getApiKey())
                                .modelName(ds.getChatModel())
                                .timeout(java.time.Duration.ofSeconds(60))
                                .build();
                    }
                }
            }
        }
        return defaultChatModel;
    }

    /**
     * 流式模型（默认，用于代码生成）
     */
    public StreamingChatModel streamingChatModel() {
        if (defaultStreamingChatModel == null) {
            synchronized (this) {
                if (defaultStreamingChatModel == null) {
                    if (isMock()) {
                        log.info("[AI] 使用 Mock StreamingChatModel");
                        defaultStreamingChatModel = new MockStreamingChatModel();
                    } else {
                        AiProperties.DeepSeek ds = aiProperties.getDeepseek();
                        defaultStreamingChatModel = OpenAiStreamingChatModel.builder()
                                .baseUrl(ds.getBaseUrl())
                                .apiKey(ds.getApiKey())
                                .modelName(ds.getChatModel())
                                .timeout(java.time.Duration.ofSeconds(aiProperties.getTimeoutSeconds()))
                                .build();
                    }
                }
            }
        }
        return defaultStreamingChatModel;
    }

    public static BusinessException configError() {
        return new BusinessException(ErrorCode.AI_GENERATION_ERROR, "AI 模型未配置：请设置 DEEPSEEK_API_KEY 或开启 mock 模式");
    }
}
