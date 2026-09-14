package com.aicoding.core.workflow;

import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * 代码生成工作流状态（LangGraph4j AgentState）
 */
public class WFState extends AgentState {

    public WFState(Map<String, Object> initData) {
        super(initData);
    }

    public static Map<String, Channel<?>> mapFactory() {
        // 不声明 reducer 的键一律走默认 last-value 合并策略
        return Map.of();
    }

    public Long appId() {
        return value("appId").map(v -> (Long) v).orElse(null);
    }

    public Long userId() {
        return value("userId").map(v -> (Long) v).orElse(null);
    }

    public String userMessage() {
        return value("userMessage").map(Object::toString).orElse(null);
    }

    public Boolean firstGeneration() {
        return value("firstGeneration").map(v -> (Boolean) v).orElse(null);
    }

    public String genType() {
        return value("genType").map(Object::toString).orElse(null);
    }

    public Integer round() {
        return value("round").map(v -> (Integer) v).orElse(0);
    }

    public String code() {
        return value("code").map(Object::toString).orElse(null);
    }

    @SuppressWarnings("unchecked")
    public List<String> issues() {
        return value("issues").map(v -> (List<String>) v).orElse(null);
    }

    public SseEmitter emitter() {
        return value("emitter").map(v -> (SseEmitter) v).orElse(null);
    }
}
