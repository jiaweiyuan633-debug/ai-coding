package com.aicoding.service.impl;

import cn.hutool.json.JSONUtil;
import com.aicoding.mapper.WorkflowRunMapper;
import com.aicoding.model.entity.WorkflowRun;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 工作流执行审计：节点状态持久化（用于回放与监控）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowAuditService {

    private final WorkflowRunMapper workflowRunMapper;

    public void record(long appId, String runId, String node, String status, Object payload, long costMs) {
        try {
            WorkflowRun run = new WorkflowRun();
            run.setAppId(appId);
            run.setRunId(runId);
            run.setNode(node);
            run.setStatus(status);
            run.setPayload(payload == null ? null : JSONUtil.toJsonStr(payload));
            run.setCostMs(costMs);
            workflowRunMapper.insert(run);
        } catch (Exception e) {
            log.warn("[Workflow] 审计记录失败: {}", e.getMessage());
        }
    }
}
