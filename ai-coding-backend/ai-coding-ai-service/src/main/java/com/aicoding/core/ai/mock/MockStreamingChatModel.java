package com.aicoding.core.ai.mock;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.TokenUsage;

/**
 * Mock 流式模型：按 40ms 间隔逐块吐出演示 HTML，模拟真实流式生成
 */
public class MockStreamingChatModel implements StreamingChatModel {

    private static final long CHUNK_DELAY_MS = 40;

    @Override
    public void doChat(ChatRequest chatRequest, StreamingChatResponseHandler handler) {
        String full = MockContents.indexHtml();
        String wrapped = "```html\n" + full + "\n```";
        Thread.startVirtualThread(() -> {
            try {
                int chunkSize = 24;
                for (int i = 0; i < wrapped.length(); i += chunkSize) {
                    handler.onPartialResponse(wrapped.substring(i, Math.min(wrapped.length(), i + chunkSize)));
                    Thread.sleep(CHUNK_DELAY_MS);
                }
                handler.onCompleteResponse(ChatResponse.builder()
                        .aiMessage(AiMessage.from(wrapped))
                        .tokenUsage(new TokenUsage(200, wrapped.length() / 4))
                        .finishReason(FinishReason.STOP)
                        .build());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                handler.onError(e);
            } catch (Exception e) {
                handler.onError(e);
            }
        });
    }
}
