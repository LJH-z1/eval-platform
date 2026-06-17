package com.mavis.evalplatform.auth.service;

import com.mavis.evalplatform.auth.dto.LoginRequest;
import com.mavis.evalplatform.auth.dto.LoginResponse;
import com.mavis.evalplatform.auth.entity.User;
import com.mavis.evalplatform.auth.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 认证服务 — 实现骨架
 * <p>
 * 由【刘家豪 FR-01】完成实现,其他人请勿修改。
 * <p>
 * 业务规则(对齐需求规格说明书 §3.1.5):
 * <ul>
 *   <li>用户名 3-20 位,密码 6-20 位(至少字母+数字)</li>
 *   <li>连续 5 次密码错误 → 锁定 30 分钟</li>
 *   <li>登录成功后签发 JWT,8 小时有效</li>
 *   <li>登录失败 / 锁定 / 注销均记录到 audit_log</li>
 * </ul>
 *
 * @author 刘家豪
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_FAILED = 5;
    private static final int LOCK_MINUTES = 30;

    private final UserMapper userMapper;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditLogService auditLogService;

    @Value("${eval.jwt.expire-hours:8}")
    private long expireHours;

    /**
     * 用户登录
     * <p>
     * 实现提示:
     * <ol>
     *   <li>先 {@code UserService.validateUsername/validatePassword} 校验格式</li>
     *   <li>查 user 表,不存在 → 抛 PASSWORD_INCORRECT(防用户名枚举)</li>
     *   <li>检查 lockedUntil:未过期 → 抛 USER_LOCKED</li>
     *   <li>检查 status:0 → 抛 USER_DISABLED</li>
     *   <li>BCrypt 校验密码,失败累加 failed_count,达 5 锁定</li>
     *   <li>成功 → 重置 failed_count,签发 JWT,写 audit_log(LOGIN/SUCCESS)</li>
     * </ol>
     */
    @Transactional
    public LoginResponse login(LoginRequest req, String ip) {
        // TODO 由刘家豪实现
        throw new UnsupportedOperationException("TODO 由刘家豪 FR-01 实现 login");
    }

    public void logout(Long userId, String username) {
        // TODO 由刘家豪实现:JWT 无状态,客户端删 token 即可,服务端仅写 audit_log(LOGOUT/SUCCESS)
        throw new UnsupportedOperationException("TODO 由刘家豪 FR-01 实现 logout");
    }
}
