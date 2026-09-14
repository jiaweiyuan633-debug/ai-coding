package com.aicoding.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 模型配置
 */
@Data
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    /**
     * 是否启用 Mock 模式（无 API Key 时联调用）
     */
    private boolean mockEnabled = true;

    private DeepSeek deepseek = new DeepSeek();

    /**
     * 生成超时（秒）
     */
    private int timeoutSeconds = 300;

    @Data
    public static class DeepSeek {
        private String baseUrl = "https://api.deepseek.com";
        private String apiKey = "";
        private String chatModel = "deepseek-chat";
    }
}
