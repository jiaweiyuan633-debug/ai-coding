package com.aicoding.service.impl;

import com.aicoding.common.BusinessException;
import com.aicoding.common.ErrorCode;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 应用封面服务：为应用生成渐变风格 SVG 封面（本地零依赖方案）
 * 真实浏览器截图将在微服务阶段的 screenshot-service 中接入
 */
@Service
public class CoverServiceImpl implements com.aicoding.service.CoverService {

    private static final List<String[]> GRADIENTS = List.of(
            new String[]{"#667eea", "#764ba2"},
            new String[]{"#f093fb", "#f5576c"},
            new String[]{"#4facfe", "#00f2fe"},
            new String[]{"#43e97b", "#38f9d7"},
            new String[]{"#fa709a", "#fee140"},
            new String[]{"#30cfd0", "#330867"});

    @Getter
    private final Path coverRoot = Paths.get("./tmp/covers").toAbsolutePath().normalize();

    @Override
    public String generateCover(long appId, String appName) {
        String[] colors = GRADIENTS.get((int) (Math.abs(appId) % GRADIENTS.size()));
        String title = escape(appName == null ? "未命名应用" : appName);
        if (title.length() > 12) {
            title = title.substring(0, 12) + "…";
        }
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="640" height="360" viewBox="0 0 640 360">
                  <defs>
                    <linearGradient id="g" x1="0" y1="0" x2="1" y2="1">
                      <stop offset="0%%" stop-color="%s"/>
                      <stop offset="100%%" stop-color="%s"/>
                    </linearGradient>
                  </defs>
                  <rect width="640" height="360" fill="url(#g)" rx="24"/>
                  <circle cx="560" cy="60" r="90" fill="rgba(255,255,255,0.12)"/>
                  <circle cx="80" cy="300" r="120" fill="rgba(255,255,255,0.10)"/>
                  <text x="320" y="196" font-family="PingFang SC, Microsoft YaHei, sans-serif" font-size="40"
                        fill="#ffffff" text-anchor="middle" font-weight="bold">%s</text>
                  <text x="320" y="240" font-family="sans-serif" font-size="16"
                        fill="rgba(255,255,255,0.85)" text-anchor="middle">AI Coding 生成应用</text>
                </svg>
                """.formatted(colors[0], colors[1], title);
        try {
            Files.createDirectories(coverRoot);
            Path file = coverRoot.resolve(appId + ".svg");
            Files.writeString(file, svg, StandardCharsets.UTF_8);
            return file.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成封面失败");
        }
    }

    public Path coverPath(long appId) {
        return coverRoot.resolve(appId + ".svg");
    }

    private String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
