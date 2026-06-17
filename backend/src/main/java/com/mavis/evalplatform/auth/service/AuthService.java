package com.mavis.evalplatform.auth.service;

import com.mavis.evalplatform.auth.dto.LoginRequest;
import com.mavis.evalplatform.auth.dto.LoginResponse;
import com.mavis.evalplatform.auth.entity.User;
import com.mavis.evalplatform.auth.mapper.UserMapper;
import com.mavis.evalplatform.common.exception.BusinessException;
import com.mavis.evalplatform.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 认证服务 — 最小可用实现
 *
 * @author 刘家豪
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_FAILED = 5;

    private final UserMapper userMapper;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${eval.jwt.expire-hours:8}")
    private long expireHours;

    @Transactional
    public LoginResponse login(LoginRequest req, String ip) {
        UserService.validateUsername(req.getUsername());
        UserService.validatePassword(req.getPassword());

        User u = userMapper.selectByUsername(req.getUsername());
        if (u == null) {
            throw new BusinessException(ErrorCode.PASSWORD_INCORRECT);
        }
        if (u.getLockedUntil() != null && u.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.USER_LOCKED);
        }
        if (u.getStatus() == null || u.getStatus() == 0) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
        if (!passwordEncoder.matches(req.getPassword(), u.getPassword())) {
            int newFailed = (u.getFailedCount() == null ? 0 : u.getFailedCount()) + 1;
            userMapper.incrementFailedCount(u.getId());
            if (newFailed >= MAX_FAILED) {
                User upd = new User();
                upd.setId(u.getId());
                upd.setLockedUntil(LocalDateTime.now().plusMinutes(30));
                userMapper.updateById(upd);
                throw new BusinessException(ErrorCode.USER_LOCKED);
            }
            throw new BusinessException(ErrorCode.PASSWORD_INCORRECT);
        }

        String token = jwtService.generateToken(u.getId(), u.getUsername(), u.getRole());
        userMapper.resetFailedCount(u.getId());

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(expireHours * 3600L)
                .userInfo(userService.toUserInfo(u))
                .build();
    }

    public void logout(Long userId, String username) {
        // JWT 无状态,客户端清空即可
        log.info("[AuthService] logout user={}", username);
    }
}
