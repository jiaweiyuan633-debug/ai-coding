package com.aicoding.service;

import com.aicoding.common.PageResult;
import com.aicoding.model.dto.app.AppAddRequest;
import com.aicoding.model.dto.app.AppQueryRequest;
import com.aicoding.model.dto.app.AppUpdateRequest;
import com.aicoding.model.dto.app.AppVO;
import com.aicoding.model.entity.App;
import com.aicoding.model.entity.User;

/**
 * 应用服务
 */
public interface AppService {

    long addApp(AppAddRequest request, User user);

    void updateApp(AppUpdateRequest request, User user);

    void deleteApp(long appId, User user);

    AppVO getAppVO(long appId);

    App getAppById(long appId);

    PageResult<AppVO> pageMyApps(User user, AppQueryRequest request);

    PageResult<AppVO> pageSquareApps(AppQueryRequest request);

    void checkAppOwner(App app, User user);

    /**
     * 生成完成后回写应用元信息（自动命名等）
     */
    void updateGeneratedMeta(App app);
}
