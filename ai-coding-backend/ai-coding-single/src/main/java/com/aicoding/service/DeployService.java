package com.aicoding.service;

import com.aicoding.model.entity.App;
import com.aicoding.model.entity.User;

import java.nio.file.Path;

/**
 * 一键部署服务
 */
public interface DeployService {

    /**
     * 部署应用到静态托管目录，返回更新了 deployKey/deployedTime 的应用
     */
    App deploy(long appId, User user);

    /**
     * deployKey 对应的静态托管目录
     */
    Path deployDir(String deployKey);
}
