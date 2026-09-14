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

/**
 * AI 模型管理器（策略模式）：统一产出 ChatModel / StreamingChatModel。
 * Mock 模式用于无 API Key 时开发联调；正式模式对接 DeepSeek（OpenAI 兼容协议）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiModelManager {

    private final AiProperties aiProperties;

    private volatile ChatModel chatModel;
    private volatile StreamingChatModel streamingChatModel;

    public boolean isMock() {
        return aiProperties.isMockEnabled() || StringUtils.isBlank(aiProperties.getDeepseek().getApiKey());
    }

    /**
     * 非流式模型（用于路由决策等短请求）
     */
    public ChatModel chatModel() {
        if (chatModel == null) {
            synchronized (this) {
                if (chatModel == null) {
                    if (isMock()) {
                        log.info("[AI] 使用 Mock ChatModel");
                        chatModel = new MockChatModel();
                    } else {
                        AiProperties.DeepSeek ds = aiProperties.getDeepseek();
                        chatModel = OpenAiChatModel.builder()
                                .baseUrl(ds.getBaseUrl())
                                .apiKey(ds.getApiKey())
                                .modelName(ds.getChatModel())
                                .timeout(java.time.Duration.ofSeconds(60))
                                .build();
                    }
                }
            }
        }
        return chatModel;
    }

    /**
     * 流式模型（用于代码生成）
     */
    public StreamingChatModel streamingChatModel() {
        if (streamingChatModel == null) {
            synchronized (this) {
                if (streamingChatModel == null) {
                    if (isMock()) {
                        log.info("[AI] 使用 Mock StreamingChatModel");
                        streamingChatModel = new MockStreamingChatModel();
                    } else {
                        AiProperties.DeepSeek ds = aiProperties.getDeepseek();
                        streamingChatModel = OpenAiStreamingChatModel.builder()
                                .baseUrl(ds.getBaseUrl())
                                .apiKey(ds.getApiKey())
                                .modelName(ds.getChatModel())
                                .timeout(java.time.Duration.ofSeconds(aiProperties.getTimeoutSeconds()))
                                .build();
                    }
                }
            }
        }
        return streamingChatModel;
    }

    public static BusinessException configError() {
        return new BusinessException(ErrorCode.AI_GENERATION_ERROR, "AI 模型未配置：请设置 DEEPSEEK_API_KEY 或开启 mock 模式");
    }
}
