package com.aicoding.core.ai.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiConsumer;

/**
 * 多文件模式的 writeFile 工具：AI 通过工具调用将文件写入应用工作目录。
 * 每次生成请求创建独立实例，天然隔离并发。
 */
@Slf4j
public class WriteFileTool {

    private final Path appDir;

    private final BiConsumer<String, String> toolListener;

    @Getter
    private int fileCount = 0;

    public WriteFileTool(Path appDir, BiConsumer<String, String> toolListener) {
        this.appDir = appDir;
        this.toolListener = toolListener;
    }

    @Tool("将一个代码文件写入应用目录。path 为相对路径（如 index.html、css/style.css、js/main.js），content 为完整文件内容")
    public String writeFile(@P("相对文件路径") String path, @P("完整文件内容") String content) {
        try {
            if (path == null || path.isBlank()) {
                return "失败：文件路径不能为空";
            }
            Path target = appDir.resolve(path).normalize();
            if (!target.startsWith(appDir)) {
                return "失败：非法路径 " + path;
            }
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
            Files.writeString(target, content == null ? "" : content, StandardCharsets.UTF_8);
            fileCount++;
            if (toolListener != null) {
                toolListener.accept(path, content == null ? "" : content);
            }
            log.info("[WriteFileTool] {} 写入成功（{} 字符）", path, content == null ? 0 : content.length());
            return "成功：" + path;
        } catch (Exception e) {
            log.error("[WriteFileTool] 写入失败: {}", path, e);
            return "失败：" + e.getMessage();
        }
    }
}
