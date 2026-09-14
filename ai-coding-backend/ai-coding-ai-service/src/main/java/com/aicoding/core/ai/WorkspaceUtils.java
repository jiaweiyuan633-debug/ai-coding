package com.aicoding.core.ai;

import com.aicoding.common.BusinessException;
import com.aicoding.common.ErrorCode;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

/**
 * 代码工作区管理：每个应用一个目录，生成产物落盘于此
 */
@Getter
@Component
public class WorkspaceUtils {

    private final Path workspaceRoot;

    public WorkspaceUtils(@Value("${app.workspace-path:./tmp/workdir}") String workspacePath) {
        this.workspaceRoot = Paths.get(workspacePath).toAbsolutePath().normalize();
    }

    public Path appDir(long appId) {
        Path dir = workspaceRoot.resolve(String.valueOf(appId));
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建应用工作目录失败");
        }
        return dir;
    }

    /**
     * 写入文件（相对路径，禁止路径穿越）
     */
    public Path writeFile(long appId, String relativePath, String content) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件路径不能为空");
        }
        Path dir = appDir(appId);
        Path target = dir.resolve(relativePath).normalize();
        if (!target.startsWith(dir)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "非法文件路径：" + relativePath);
        }
        try {
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
            Files.writeString(target, content == null ? "" : content, StandardCharsets.UTF_8);
            return target;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "写文件失败：" + relativePath);
        }
    }

    public String readFile(long appId, String relativePath) {
        Path dir = appDir(appId);
        Path target = dir.resolve(relativePath).normalize();
        if (!target.startsWith(dir) || !Files.exists(target)) {
            return null;
        }
        try {
            return Files.readString(target, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 复制应用工作区（Remix 用）
     */
    public void copyWorkspace(long sourceAppId, long targetAppId) {
        Path source = appDir(sourceAppId);
        Path target = appDir(targetAppId);
        try (Stream<Path> stream = Files.walk(source)) {
            stream.forEach(src -> {
                try {
                    Path dest = target.resolve(source.relativize(src));
                    if (Files.isDirectory(src)) {
                        Files.createDirectories(dest);
                    } else {
                        Files.createDirectories(dest.getParent());
                        Files.copy(src, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException ignored) {
                }
            });
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "复制应用产物失败");
        }
    }

    /**
     * 列出应用目录下全部相对路径
     */
    public List<String> listFiles(long appId) {
        Path dir = appDir(appId);
        if (!Files.exists(dir)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.walk(dir)) {
            return stream.filter(Files::isRegularFile)
                    .map(p -> dir.relativize(p).toString().replace('\\', '/'))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }
}
