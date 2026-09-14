package com.aicoding.service;

import com.aicoding.model.dto.user.LoginVO;
import com.aicoding.model.dto.user.UserLoginRequest;
import com.aicoding.model.dto.user.UserRegisterRequest;
import com.aicoding.model.dto.user.UserVO;
import com.aicoding.model.entity.User;

/**
 * 用户服务
 */
public interface UserService {

    long register(UserRegisterRequest request);

    LoginVO login(UserLoginRequest request);

    User getById(long id);

    UserVO toVO(User user);
}
