package com.aicoding.core.ai;

/**
 * 流式代码解析器：将模型输出的代码增量清洗后透传
 * （剥离 markdown 代码围栏 ```html / ```，按行处理保证围栏跨越分块时依然正确）
 */
public class CodeStreamParser {

    private final StringBuilder cleaned = new StringBuilder();

    private final StringBuilder lineBuffer = new StringBuilder();

    private boolean fenceOpen = false;

    private boolean seenFirstCode = false;

    /**
     * 处理一个增量块，返回清洗后应当推送的内容（可能为空串）
     */
    public synchronized String onDelta(String chunk) {
        if (chunk == null || chunk.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < chunk.length(); i++) {
            char c = chunk.charAt(i);
            if (c == '\n') {
                lineBuffer.append(c);
                out.append(processLine());
            } else {
                lineBuffer.append(c);
            }
        }
        cleaned.append(out);
        return out.toString();
    }

    private String processLine() {
        String line = lineBuffer.toString();
        lineBuffer.setLength(0);
        String trimmed = line.trim();
        if (trimmed.startsWith("```")) {
            fenceOpen = !fenceOpen;
            return "";
        }
        if (!seenFirstCode) {
            if (trimmed.isEmpty()) {
                return "";
            }
            seenFirstCode = true;
        }
        return line;
    }

    /**
     * 流结束时返回完整清洗后的内容
     */
    public synchronized String complete() {
        if (lineBuffer.length() > 0) {
            cleaned.append(processLine());
        }
        return cleaned.toString();
    }
}
