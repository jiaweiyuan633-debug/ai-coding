package com.aicoding.service;

import com.aicoding.model.entity.AppVersion;

import java.util.List;

/**
 * 版本时光机服务
 */
public interface AppVersionService {

    /**
     * 为应用当前工作区内容创建版本快照
     */
    AppVersion createSnapshot(long appId, long userId, String message, String genType);

    /**
     * 版本列表（不含快照内容）
     */
    List<AppVersion> listVersions(long appId);

    /**
     * 回滚到指定版本：恢复文件到工作区并生成新版本记录
     */
    AppVersion rollback(long appId, int version, long userId);
}
