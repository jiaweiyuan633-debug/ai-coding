package com.aicoding.controller;

import com.aicoding.common.BaseResponse;
import com.aicoding.common.ResultUtils;
import com.aicoding.core.auth.UserContext;
import com.aicoding.mapper.ModelInvocationMapper;
import com.aicoding.model.entity.ModelInvocation;
import com.aicoding.model.entity.User;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用量中心接口
 */
@RestController
@RequestMapping("/usage")
@RequiredArgsConstructor
public class UsageController {

    private final ModelInvocationMapper modelInvocationMapper;

    /**
     * 用量汇总：普通用户看自己；管理员可指定 appId/userId 或查全站
     */
    @GetMapping("/summary")
    public BaseResponse<Map<String, Object>> summary(@RequestParam(required = false) Long appId,
                                                     @RequestParam(required = false) Long userId) {
        User current = UserContext.get();
        boolean admin = "admin".equals(current.getUserRole());
        LambdaQueryWrapper<ModelInvocation> wrapper = new LambdaQueryWrapper<>();
        if (admin) {
            wrapper.eq(appId != null, ModelInvocation::getAppId, appId);
            wrapper.eq(userId != null, ModelInvocation::getUserId, userId);
        } else {
            wrapper.eq(ModelInvocation::getUserId, current.getId());
            if (appId != null) {
                wrapper.eq(ModelInvocation::getAppId, appId);
            }
        }
        wrapper.orderByDesc(ModelInvocation::getCreateTime).last("limit 500");
        List<ModelInvocation> records = modelInvocationMapper.selectList(wrapper);

        long inputTokens = 0;
        long outputTokens = 0;
        long costMs = 0;
        long failed = 0;
        Map<String, long[]> byPurpose = new LinkedHashMap<>();
        for (ModelInvocation record : records) {
            inputTokens += nullToZero(record.getInputTokens());
            outputTokens += nullToZero(record.getOutputTokens());
            costMs += record.getCostMs() == null ? 0 : record.getCostMs();
            if ("failed".equals(record.getStatus())) {
                failed++;
            }
            String purpose = record.getPurpose() == null ? "other" : record.getPurpose();
            long[] agg = byPurpose.computeIfAbsent(purpose, k -> new long[3]);
            agg[0]++;
            agg[1] += nullToZero(record.getInputTokens()) + nullToZero(record.getOutputTokens());
            agg[2] += record.getCostMs() == null ? 0 : record.getCostMs();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalInvocations", records.size());
        result.put("inputTokens", inputTokens);
        result.put("outputTokens", outputTokens);
        result.put("totalTokens", inputTokens + outputTokens);
        result.put("totalCostMs", costMs);
        result.put("failedInvocations", failed);
        result.put("byPurpose", byPurpose);
        return ResultUtils.success(result);
    }

    private long nullToZero(Integer value) {
        return value == null ? 0 : value;
    }
}
