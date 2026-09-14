package com.aicoding.service.impl;

import com.aicoding.common.BusinessException;
import com.aicoding.common.ErrorCode;
import com.aicoding.common.PageResult;
import com.aicoding.mapper.AppMapper;
import com.aicoding.mapper.UserMapper;
import com.aicoding.model.dto.app.AppAddRequest;
import com.aicoding.model.dto.app.AppQueryRequest;
import com.aicoding.model.dto.app.AppUpdateRequest;
import com.aicoding.model.dto.app.AppVO;
import com.aicoding.model.dto.user.UserVO;
import com.aicoding.model.entity.App;
import com.aicoding.model.entity.User;
import com.aicoding.service.AppService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 应用服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppServiceImpl implements AppService {

    private final AppMapper appMapper;
    private final UserMapper userMapper;
    private final com.aicoding.core.ai.WorkspaceUtils workspaceUtils;

    @Override
    public long addApp(AppAddRequest request, User user) {
        App app = new App();
        app.setInitPrompt(request.getInitPrompt());
        app.setAppName(StringUtils.defaultIfBlank(request.getAppName(), "未命名应用"));
        app.setUserId(user.getId());
        app.setPriority(0);
        app.setIsFeatured(0);
        appMapper.insert(app);
        log.info("[App] 创建应用: id={}, userId={}", app.getId(), user.getId());
        return app.getId();
    }

    @Override
    public void updateApp(AppUpdateRequest request, User user) {
        App app = getAppById(request.getId());
        checkAppOwner(app, user);
        boolean admin = "admin".equals(user.getUserRole());
        if (StringUtils.isNotBlank(request.getAppName())) {
            app.setAppName(request.getAppName());
        }
        if (request.getCover() != null) {
            app.setCover(request.getCover());
        }
        if (request.getPriority() != null) {
            if (!admin) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅管理员可设置优先级");
            }
            app.setPriority(request.getPriority());
        }
        if (request.getIsFeatured() != null) {
            if (!admin) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅管理员可设置精选");
            }
            app.setIsFeatured(request.getIsFeatured());
        }
        appMapper.updateById(app);
    }

    @Override
    public void deleteApp(long appId, User user) {
        App app = getAppById(appId);
        checkAppOwner(app, user);
        appMapper.deleteById(appId);
        log.info("[App] 删除应用: id={}, userId={}", appId, user.getId());
    }

    @Override
    public AppVO getAppVO(long appId) {
        App app = getAppById(appId);
        AppVO vo = new AppVO();
        BeanUtils.copyProperties(app, vo);
        vo.setIsFeatured(app.getIsFeatured());
        return vo;
    }

    @Override
    public App getAppById(long appId) {
        App app = appMapper.selectById(appId);
        if (app == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        }
        return app;
    }

    @Override
    public PageResult<AppVO> pageMyApps(User user, AppQueryRequest request) {
        request.setUserId(user.getId());
        return pageApps(request);
    }

    @Override
    public PageResult<AppVO> pageSquareApps(AppQueryRequest request) {
        request.setIsFeatured(1);
        request.setSortField("priority");
        request.setSortOrder("desc");
        return pageApps(request);
    }

    @Override
    public void checkAppOwner(App app, User user) {
        if (!Objects.equals(app.getUserId(), user.getId()) && !"admin".equals(user.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作该应用");
        }
    }

    @Override
    public void updateGeneratedMeta(App app) {
        appMapper.updateById(app);
    }

    @Override
    public long remixApp(long appId, User user) {
        App source = getAppById(appId);
        App copy = new App();
        copy.setInitPrompt(source.getInitPrompt());
        copy.setAppName(source.getAppName() + " (Remix)");
        copy.setCodeGenType(source.getCodeGenType());
        copy.setUserId(user.getId());
        copy.setPriority(0);
        copy.setIsFeatured(0);
        appMapper.insert(copy);
        workspaceUtils.copyWorkspace(appId, copy.getId());
        log.info("[App] Remix: {} -> {}（userId={}）", appId, copy.getId(), user.getId());
        return copy.getId();
    }

    private PageResult<AppVO> pageApps(AppQueryRequest request) {
        LambdaQueryWrapper<App> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(request.getUserId() != null, App::getUserId, request.getUserId());
        wrapper.like(StringUtils.isNotBlank(request.getAppName()), App::getAppName, request.getAppName());
        wrapper.eq(StringUtils.isNotBlank(request.getCodeGenType()), App::getCodeGenType, request.getCodeGenType());
        wrapper.eq(request.getIsFeatured() != null, App::getIsFeatured, request.getIsFeatured());
        boolean asc = "asc".equalsIgnoreCase(request.getSortOrder());
        if ("priority".equals(request.getSortField())) {
            wrapper.orderBy(true, asc, App::getPriority).orderBy(true, false, App::getCreateTime);
        } else {
            wrapper.orderBy(true, false, App::getCreateTime);
        }
        Page<App> page = appMapper.selectPage(new Page<>(request.getCurrentPage(), request.getPageSize()), wrapper);

        Map<Long, User> userMap = batchLoadUsers(page.getRecords());
        List<AppVO> voList = page.getRecords().stream().map(app -> {
            AppVO vo = new AppVO();
            BeanUtils.copyProperties(app, vo);
            User owner = userMap.get(app.getUserId());
            if (owner != null) {
                UserVO userVO = new UserVO();
                userVO.setId(owner.getId());
                userVO.setUserName(owner.getUserName());
                userVO.setUserAvatar(owner.getUserAvatar());
                vo.setUser(userVO);
            }
            return vo;
        }).toList();

        PageResult<AppVO> result = new PageResult<>();
        result.setTotal(page.getTotal());
        result.setRecords(voList);
        result.setCurrentPage(request.getCurrentPage());
        result.setPageSize(request.getPageSize());
        return result;
    }

    private Map<Long, User> batchLoadUsers(List<App> apps) {
        List<Long> userIds = apps.stream().map(App::getUserId).filter(Objects::nonNull).distinct().toList();
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }
}
