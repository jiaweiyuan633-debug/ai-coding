package com.aicoding.api;

import com.aicoding.api.dto.AppDTO;
import com.aicoding.api.dto.AppMetaDTO;

/**
 * 应用服务 RPC 接口（ai-service 远程调用 app-service）
 */
public interface AppRpcService {

    AppDTO getApp(long appId);

    /**
     * 校验应用归属（无权限时抛出异常语义：返回 false）
     */
    boolean isOwner(long appId, long userId);

    /**
     * 生成完成后回写元信息（自动命名 / 生成类型 / 封面）
     */
    void saveGeneratedMeta(AppMetaDTO meta);

    /**
     * 创建版本快照（读取共享工作区文件），返回新版本号
     */
    int createVersionSnapshot(long appId, long userId, String message, String genType);
}
