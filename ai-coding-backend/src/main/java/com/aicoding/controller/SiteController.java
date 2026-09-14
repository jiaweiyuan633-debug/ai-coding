package com.aicoding.controller;

import com.aicoding.service.DeployService;
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
 * 部署站点访问（公开分享链接）：/s/{deployKey}/**
 */
@RestController
@RequestMapping("/s")
@RequiredArgsConstructor
public class SiteController {

    private final DeployService deployService;

    @GetMapping("/{deployKey}/**")
    public ResponseEntity<byte[]> serve(@PathVariable String deployKey, jakarta.servlet.http.HttpServletRequest request) {
        Path dir = deployService.deployDir(deployKey);
        String prefix = "/api/s/" + deployKey + "/";
        String uri = request.getRequestURI();
        String relativePath = uri.length() > prefix.length() ? uri.substring(prefix.length()) : "index.html";
        if (relativePath.isBlank() || relativePath.endsWith("/")) {
            relativePath = relativePath + "index.html";
        }
        Path target = dir.resolve(relativePath).normalize();
        if (!target.startsWith(dir) || !Files.exists(target) || !Files.isRegularFile(target)) {
            return ResponseEntity.notFound().build();
        }
        try {
            byte[] body = Files.readAllBytes(target);
            return ResponseEntity.ok().contentType(resolveMediaType(relativePath)).body(body);
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
