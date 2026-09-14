package com.aicoding.controller;

import com.aicoding.common.BaseResponse;
import com.aicoding.common.ResultUtils;
import com.aicoding.mapper.TemplateMapper;
import com.aicoding.model.entity.App;
import com.aicoding.model.entity.Template;
import com.aicoding.service.AppService;
import com.aicoding.service.impl.CoverServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 模板广场与应用封面（公开访问）
 */
@RestController
@RequiredArgsConstructor
public class PublicController {

    private final TemplateMapper templateMapper;
    private final CoverServiceImpl coverService;
    private final AppService appService;
    private final com.aicoding.cache.MultiLevelCache multiLevelCache;

    @GetMapping("/template/list")
    public BaseResponse<List<Template>> listTemplates() {
        // 多级缓存：Caffeine(60s) -> Redis(10min) -> DB
        List<Template> templates = multiLevelCache.get("tpl:list", List.class,
                () -> templateMapper.selectList(new LambdaQueryWrapper<Template>().orderByAsc(Template::getSort)));
        return ResultUtils.success(templates);
    }

    /**
     * 应用封面（缺失时按应用信息懒生成）
     */
    @GetMapping("/cover/{appId}")
    public ResponseEntity<byte[]> cover(@PathVariable long appId) {
        Path path = coverService.coverPath(appId);
        if (!Files.exists(path)) {
            try {
                App app = appService.getAppById(appId);
                coverService.generateCover(appId, app.getAppName());
            } catch (Exception ignore) {
                return ResponseEntity.notFound().build();
            }
        }
        try {
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("image/svg+xml"))
                    .body(Files.readAllBytes(path));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
