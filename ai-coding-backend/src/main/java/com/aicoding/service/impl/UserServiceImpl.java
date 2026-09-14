package com.aicoding.service.impl;

import com.aicoding.common.BusinessException;
import com.aicoding.common.ErrorCode;
import com.aicoding.core.auth.JwtUtils;
import com.aicoding.mapper.UserMapper;
import com.aicoding.model.dto.user.LoginVO;
import com.aicoding.model.dto.user.UserLoginRequest;
import com.aicoding.model.dto.user.UserRegisterRequest;
import com.aicoding.model.dto.user.UserVO;
import com.aicoding.model.entity.User;
import com.aicoding.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public long register(UserRegisterRequest request) {
        String account = request.getUserAccount();
        if (StringUtils.isBlank(account) || account.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号至少 4 位");
        }
        if (request.getUserPassword() == null || request.getUserPassword().length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码至少 8 位");
        }
        if (!request.getUserPassword().equals(request.getCheckPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUserAccount, account));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该账号已被注册");
        }
        User user = new User();
        user.setUserAccount(account);
        user.setUserPassword(passwordEncoder.encode(request.getUserPassword()));
        user.setUserName("用户" + account.substring(0, Math.min(4, account.length())));
        user.setUserRole("user");
        userMapper.insert(user);
        log.info("[User] 注册成功: {}", account);
        return user.getId();
    }

    @Override
    public LoginVO login(UserLoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUserAccount, request.getUserAccount()));
        if (user == null || !passwordEncoder.matches(request.getUserPassword(), user.getUserPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码错误");
        }
        LoginVO vo = new LoginVO();
        vo.setToken(jwtUtils.createToken(user.getId(), user.getUserRole()));
        vo.setUser(toVO(user));
        return vo;
    }

    @Override
    public User getById(long id) {
        return userMapper.selectById(id);
    }

    @Override
    public UserVO toVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUserAccount(user.getUserAccount());
        vo.setUserName(user.getUserName());
        vo.setUserAvatar(user.getUserAvatar());
        vo.setUserProfile(user.getUserProfile());
        vo.setUserRole(user.getUserRole());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}
