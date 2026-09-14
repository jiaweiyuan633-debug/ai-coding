package com.aicoding.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 应用版本快照（版本时光机）
 */
@Data
@TableName(value = "app_version")
public class AppVersion implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long appId;

    /**
     * 版本号（应用内自增）
     */
    private Integer version;

    /**
     * 快照内容（文件路径 -> 文件内容 的 JSON）
     */
    private String snapshot;

    private Integer parentVersion;

    /**
     * 版本说明（触发本次修改的对话）
     */
    private String message;

    private String genType;

    private Long userId;

    private LocalDateTime createTime;

    @TableLogic
    private Integer isDelete;

    private static final long serialVersionUID = 1L;
}
