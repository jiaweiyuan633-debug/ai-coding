package com.aicoding.controller;

import com.aicoding.core.ai.WorkspaceUtils;
import com.aicoding.mapper.AppMapper;
import com.aicoding.model.entity.App;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 生成应用在线预览（公开访问，供分享预览）
 */
@RestController
@RequestMapping("/preview")
@RequiredArgsConstructor
public class PreviewController {

    private final WorkspaceUtils workspaceUtils;
    private final AppMapper appMapper;

    @GetMapping("/{appId}/**")
    public ResponseEntity<byte[]> preview(@PathVariable long appId, HttpServletRequest request) {
        App app = appMapper.selectById(appId);
        if (app == null) {
            return ResponseEntity.notFound().build();
        }
        // 提取 /** 部分作为相对路径，默认 index.html（兼容有无 /api 前缀）
        String requestUri = request.getRequestURI();
        int marker = requestUri.indexOf("/preview/" + appId + "/");
        String relativePath = marker < 0 ? "" : requestUri.substring(marker + ("/preview/" + appId + "/").length());
        if (relativePath.isBlank() || relativePath.endsWith("/")) {
            relativePath = relativePath + "index.html";
        }
        Path dir = workspaceUtils.appDir(appId);
        Path target = dir.resolve(relativePath).normalize();
        if (!target.startsWith(dir) || !Files.exists(target) || !Files.isRegularFile(target)) {
            return ResponseEntity.notFound().build();
        }
        try {
            byte[] body = Files.readAllBytes(target);
            MediaType mediaType = resolveMediaType(relativePath);
            return ResponseEntity.ok().contentType(mediaType).body(body);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private MediaType resolveMediaType(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".html") || lower.endsWith(".htm")) {
            return MediaType.parseMediaType("text/html;charset=UTF-8");
        }
        if (lower.endsWith(".css")) {
            return MediaType.parseMediaType("text/css;charset=UTF-8");
        }
        if (lower.endsWith(".js")) {
            return MediaType.parseMediaType("application/javascript;charset=UTF-8");
        }
        if (lower.endsWith(".json")) {
            return MediaType.parseMediaType("application/json;charset=UTF-8");
        }
        if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG;
        }
        if (lower.endsWith(".svg")) {
            return MediaType.parseMediaType("image/svg+xml");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
