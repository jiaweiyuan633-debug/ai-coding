package com.aicoding.core.workflow;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 质量门禁：对生成产物做静态校验，产出问题清单（供自动修复节点使用）
 */
@Component
public class QualityGate {

    private static final Pattern LOCAL_REF = Pattern.compile(
            "(?:src|href)=[\"'](?!https?://|//|#|data:)([^\"']+)[\"']");

    /**
     * 校验单文件 HTML，返回问题列表（空列表 = 通过）
     */
    public List<String> validateSingleFile(String code) {
        List<String> issues = new ArrayList<>();
        if (code == null || code.isBlank()) {
            issues.add("代码内容为空");
            return issues;
        }
        if (code.contains("```")) {
            issues.add("代码中残留 markdown 代码围栏");
        }
        String lower = code.toLowerCase();
        if (!lower.contains("<!doctype html") && !lower.contains("<html")) {
            issues.add("缺少 HTML 文档结构（<html> 标签）");
        }
        if (!lower.contains("<head") || !lower.contains("<body")) {
            issues.add("缺少 head/body 结构");
        }
        if (countOccurrences(lower, "<script") != countOccurrences(lower, "</script>")) {
            issues.add("<script> 标签未正确闭合");
        }
        if (countOccurrences(lower, "<style") != countOccurrences(lower, "</style>")) {
            issues.add("<style> 标签未正确闭合");
        }
        return issues;
    }

    /**
     * 校验多文件应用：入口存在、引用的本地资源文件齐全
     */
    public List<String> validateMultiFile(Map<String, String> files) {
        List<String> issues = new ArrayList<>();
        if (files == null || files.isEmpty()) {
            issues.add("未生成任何文件");
            return issues;
        }
        String index = files.get("index.html");
        if (index == null || index.isBlank()) {
            issues.add("缺少入口文件 index.html");
            return issues;
        }
        List<String> indexIssues = validateSingleFile(index);
        issues.addAll(indexIssues.stream().map(i -> "index.html: " + i).toList());
        // 校验本地引用（css/js/图片）是否存在
        Matcher matcher = LOCAL_REF.matcher(index);
        while (matcher.find()) {
            String ref = matcher.group(1).split("[?#]")[0];
            String normalized = ref.replaceFirst("^\\./", "");
            if (!files.containsKey(normalized)) {
                issues.add("index.html 引用的文件不存在：%s".formatted(normalized));
            }
        }
        return issues;
    }

    private long countOccurrences(String text, String token) {
        long count = 0;
        int idx = 0;
        while ((idx = text.indexOf(token, idx)) != -1) {
            count++;
            idx += token.length();
        }
        return count;
    }
}
