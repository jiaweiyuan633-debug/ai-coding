package com.aicoding.service;

/**
 * 应用封面服务
 */
public interface CoverService {

    /**
     * 生成应用封面，返回封面文件绝对路径
     */
    String generateCover(long appId, String appName);
}
