package com.aicoding.model.dto.user;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户视图（脱敏）
 */
@Data
public class UserVO implements Serializable {

    private Long id;

    private String userAccount;

    private String userName;

    private String userAvatar;

    private String userProfile;

    private String userRole;

    private LocalDateTime createTime;

    private static final long serialVersionUID = 1L;
}
