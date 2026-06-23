package com.mavis.evalplatform.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
 * 用户业务服务 — 最小可用实现
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
        return userMapper.selectByUsername(username);
    }

    public User findById(Long id) {
        return userMapper.selectById(id);
    }

    public UserInfo toUserInfo(User u) {
        if (u == null) return null;
        UserInfo info = new UserInfo();
        info.setId(u.getId());
        info.setUsername(u.getUsername());
        info.setEmail(u.getEmail());
        info.setRole(u.getRole());
        info.setStatus(u.getStatus());
        info.setCreatedAt(u.getCreatedAt());
        Role r = Role.of(u.getRole());
        info.setRoleDescription(r.getDescription());
        return info;
    }

    @Transactional
    public void changePassword(Long userId, String oldPwd, String newPwd) {
        validatePassword(newPwd);
        User u = userMapper.selectById(userId);
        if (u == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        if (!passwordEncoder.matches(oldPwd, u.getPassword())) {
            throw new BusinessException(ErrorCode.OLD_PASSWORD_INCORRECT);
        }
        u.setPassword(passwordEncoder.encode(newPwd));
        u.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(u);
    }

    @Transactional
    public void disableUser(Long userId) {
        User u = userMapper.selectById(userId);
        if (u == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        u.setStatus(0);
        u.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(u);
    }

    @Transactional
    public void enableUser(Long userId) {
        User u = userMapper.selectById(userId);
        if (u == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        u.setStatus(1);
        u.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(u);
    }

    @Transactional
    public UserInfo createUser(RegisterRequest req) {
        validateUsername(req.getUsername());
        validatePassword(req.getPassword());
        if (userMapper.selectByUsername(req.getUsername()) != null) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }
        User u = new User();
        u.setUsername(req.getUsername());
        u.setPassword(passwordEncoder.encode(req.getPassword()));
        u.setEmail(StringUtils.hasText(req.getEmail()) ? req.getEmail() : null);
        u.setRole(Role.of(req.getRole()).getCode());
        u.setStatus(1);
        u.setFailedCount(0);
        u.setCreatedAt(LocalDateTime.now());
        u.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(u);
        return toUserInfo(u);
    }

    public PageResult<UserInfo> page(long pageNum, long pageSize, String role) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<User> p =
                com.baomidou.mybatisplus.extension.plugins.pagination.Page.of(pageNum, pageSize);
        LambdaQueryWrapper<User> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(role)) w.eq(User::getRole, role);
        w.orderByDesc(User::getCreatedAt);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<User> result = userMapper.selectPage(p, w);
        return PageResult.of(result.getRecords().stream().map(this::toUserInfo).toList(),
                result.getTotal(), pageNum, pageSize);
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
