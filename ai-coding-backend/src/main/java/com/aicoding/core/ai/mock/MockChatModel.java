package com.aicoding.core.ai.mock;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;



/**
 * Mock 非流式模型：用于本地联调（路由决策等），按关键词返回生成类型
 */
public class MockChatModel implements ChatModel {

    @Override
    public ChatResponse doChat(ChatRequest chatRequest) {
        // 取最后一条用户消息（真实需求文本）做关键词判断，避免被提示词模板中的关键词干扰
        String userText = chatRequest.messages().stream()
                .filter(UserMessage.class::isInstance)
                .map(m -> ((UserMessage) m).singleText())
                .reduce((first, second) -> second)
                .orElse("");
        String answer = (userText.contains("vue") || userText.contains("Vue") || userText.contains("组件"))
                ? "VUE_PROJECT" : "HTML_SINGLE";
        return ChatResponse.builder().aiMessage(AiMessage.from(answer)).build();
    }
}
