package com.aicoding.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工作流节点执行记录（透明工作流）
 */
@Data
@TableName(value = "workflow_run")
public class WorkflowRun implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long appId;

    private String runId;

    private String node;

    /**
     * running / success / failed
     */
    private String status;

    /**
     * 节点产物/错误摘要（JSON）
     */
    private String payload;

    private Long costMs;

    private LocalDateTime createTime;

    @TableLogic
    private Integer isDelete;

    private static final long serialVersionUID = 1L;
}
