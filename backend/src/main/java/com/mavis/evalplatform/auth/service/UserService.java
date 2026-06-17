package com.mavis.evalplatform.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mavis.evalplatform.auth.dto.RegisterRequest;
import com.mavis.evalplatform.auth.dto.UserInfo;
import com.mavis.evalplatform.auth.entity.Role;
import com.mavis.evalplatform.auth.entity.User;
import com.mavis.evalplatform.auth.mapper.UserMapper;
import com.mavis.evalplatform.common.exception.BusinessException;
import com.mavis.evalplatform.common.exception.ErrorCode;
import com.mavis.evalplatform.common.result.PageResult;
import com.mavis.evalplatform.common.util.AesUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * 用户业务服务 — 实现骨架
 * <p>
 * 由【刘家豪 FR-01】完成实现,其他人请勿修改。
 *
 * @author 刘家豪
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_.-]{3,20}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z0-9_.-]{6,20}$");

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AesUtil aesUtil;

    public User findByUsername(String username) {
        // TODO 由刘家豪实现
        throw new UnsupportedOperationException("TODO 由刘家豪 FR-01 实现 findByUsername");
    }

    public User findById(Long id) {
        // TODO 由刘家豪实现
        throw new UnsupportedOperationException("TODO 由刘家豪 FR-01 实现 findById");
    }

    public UserInfo toUserInfo(User u) {
        // TODO 由刘家豪实现:User → UserInfo(不返回 password)
        throw new UnsupportedOperationException("TODO 由刘家豪 FR-01 实现 toUserInfo");
    }

    public UserInfo createUser(RegisterRequest req) {
        // TODO 由刘家豪实现:校验 + BCrypt 加密 + 写入 + 转 UserInfo
        throw new UnsupportedOperationException("TODO 由刘家豪 FR-01 实现 createUser");
    }

    public void changePassword(Long userId, String oldPwd, String newPwd) {
        // TODO 由刘家豪实现
        throw new UnsupportedOperationException("TODO 由刘家豪 FR-01 实现 changePassword");
    }

    public void disableUser(Long userId) {
        // TODO 由刘家豪实现
        throw new UnsupportedOperationException("TODO 由刘家豪 实现 disableUser");
    }

    public PageResult<UserInfo> page(long pageNum, long pageSize, String role) {
        // TODO 由刘家豪实现
        throw new UnsupportedOperationException("TODO 由刘家豪 FR-01 实现 page");
    }

    public static void validateUsername(String username) {
        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            throw new BusinessException(ErrorCode.INVALID_USERNAME_FORMAT);
        }
    }

    public static void validatePassword(String password) {
        if (password == null || !PASSWORD_PATTERN.matcher(password).matches()) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD_FORMAT);
        }
    }
}
