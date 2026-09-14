package com.aicoding.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

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
     * 按用途路由到不同模型名（多模型路由）：router=路由决策，generate=代码生成，repair=自动修复
     */
    private Map<String, String> purposeModels = Map.of(
            "router", "deepseek-chat",
            "generate", "deepseek-chat",
            "repair", "deepseek-chat"
    );

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
