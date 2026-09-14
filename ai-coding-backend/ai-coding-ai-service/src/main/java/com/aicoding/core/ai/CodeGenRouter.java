package com.aicoding.core.ai;

import com.aicoding.model.enums.CodeGenTypeEnum;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * 代码生成类型智能路由（策略 + 兜底关键词规则）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CodeGenRouter {

    private final AiModelManager aiModelManager;

    /**
     * 决策生成类型：优先大模型，失败时关键词兜底
     */
    public CodeGenTypeEnum route(String userPrompt) {
        try {
            ChatModel chatModel = aiModelManager.chatModel();
            ChatRequest request = ChatRequest.builder()
                    .messages(List.of(SystemMessage.from(AiPrompts.ROUTER_SYSTEM), UserMessage.from(userPrompt)))
                    .build();
            String answer = chatModel.chat(request).aiMessage().text();
            CodeGenTypeEnum decided = CodeGenTypeEnum.fromValue(answer);
            if (decided != null) {
                log.info("[AI Router] 大模型决策: {} -> {}", abbreviate(userPrompt), decided.getValue());
                return decided;
            }
            log.warn("[AI Router] 大模型输出无法解析: {}", abbreviate(answer));
        } catch (Exception e) {
            log.warn("[AI Router] 大模型路由失败，使用关键词兜底: {}", e.getMessage());
        }
        return fallbackRoute(userPrompt);
    }

    private CodeGenTypeEnum fallbackRoute(String userPrompt) {
        String p = userPrompt.toLowerCase();
        if (p.contains("vue") || p.contains("组件") || p.contains("状态管理") || p.contains("路由")) {
            return CodeGenTypeEnum.VUE_PROJECT;
        }
        if (p.contains("多文件") || p.contains("复杂") || p.contains("模块") || p.length() > 300) {
            return CodeGenTypeEnum.HTML_MULTI_FILE;
        }
        return CodeGenTypeEnum.HTML_SINGLE;
    }

    private String abbreviate(String s) {
        return StringUtils.abbreviate(s, 80);
    }
}
