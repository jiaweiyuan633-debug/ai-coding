package com.aicoding.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.aicoding.common.BusinessException;
import com.aicoding.common.ErrorCode;
import com.aicoding.core.ai.WorkspaceUtils;
import com.aicoding.model.entity.App;
import com.aicoding.model.entity.User;
import com.aicoding.service.AppService;
import com.aicoding.service.DeployService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * 一键部署服务：把应用工作区产物发布到静态托管目录，以 deployKey 对外分享
 * （阶段 7 由 Nginx 泛子域名 {deployKey}.localhost 直接挂载同一目录）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeployServiceImpl implements DeployService {

    private final WorkspaceUtils workspaceUtils;
    private final AppService appService;

    @Getter
    private final Path deployRoot = Paths.get("./tmp/deploy").toAbsolutePath().normalize();

    @Override
    public App deploy(long appId, User user) {
        App app = appService.getAppById(appId);
        appService.checkAppOwner(app, user);
        List<String> files = workspaceUtils.listFiles(appId);
        if (files.isEmpty() || !files.contains("index.html")) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "应用尚无可部署的产物，请先生成");
        }
        try {
            Files.createDirectories(deployRoot);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建部署目录失败");
        }
        String deployKey = app.getDeployKey();
        if (deployKey == null || deployKey.isBlank()) {
            deployKey = RandomUtil.randomString(12);
        }
        Path targetDir = deployRoot.resolve(deployKey).normalize();
        if (!targetDir.startsWith(deployRoot)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "非法部署标识");
        }
        try {
            if (Files.exists(targetDir)) {
                deleteRecursively(targetDir);
            }
            Files.createDirectories(targetDir);
            copyRecursively(workspaceUtils.appDir(appId), targetDir);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "部署文件复制失败");
        }
        app.setDeployKey(deployKey);
        app.setDeployedTime(LocalDateTime.now());
        appService.updateGeneratedMeta(app);
        log.info("[Deploy] 应用 {} 部署完成，deployKey={}", appId, deployKey);
        return app;
    }

    @Override
    public Path deployDir(String deployKey) {
        Path dir = deployRoot.resolve(deployKey).normalize();
        if (!dir.startsWith(deployRoot)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "非法部署标识");
        }
        return dir;
    }

    private void copyRecursively(Path source, Path target) throws IOException {
        try (var walk = Files.walk(source)) {
            walk.forEach(src -> {
                try {
                    Path dest = target.resolve(source.relativize(src));
                    if (Files.isDirectory(src)) {
                        Files.createDirectories(dest);
                    } else {
                        Files.createDirectories(dest.getParent());
                        Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException ignored) {
                    // 单文件失败不阻塞整体部署
                }
            });
        }
    }

    private void deleteRecursively(Path dir) throws IOException {
        try (var walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException ignored) {
                }
            });
        }
    }
}
