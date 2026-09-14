package com.aicoding.core.ai.tools;

import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

/**
 * 多文件生成 AiService（AiServices 构建的工具调用流式接口）
 */
public interface MultiFileAiService {

    TokenStream generate(@UserMessage String userMessage);
}
