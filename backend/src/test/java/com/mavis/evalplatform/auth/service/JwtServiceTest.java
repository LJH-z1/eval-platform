package com.mavis.evalplatform.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtService 单元测试 — 骨架
 * <p>
 * 完整测试由【刘家豪 FR-01】实现。
 * <p>
 * 必测场景(对齐测试计划 §6.1):
 * <ul>
 *   <li>TC-01-001:正常生成与解析 Token</li>
 *   <li>TC-01-004:过期 Token 校验失败</li>
 *   <li>错误签名的 Token 解析失败</li>
 * </ul>
 *
 * @author 刘家豪
 */
@DisplayName("JwtService 单元测试(骨架)")
class JwtServiceTest {

    private JwtService newService() {
        JwtService s = new JwtService();
        ReflectionTestUtils.setField(s, "secret", "test-jwt-secret-must-be-at-least-32-bytes-long-2026");
        ReflectionTestUtils.setField(s, "expireHours", 8L);
        s.init();
        return s;
    }

    @Test
    @DisplayName("TODO 由刘家豪实现:TC-01-001")
    void placeholder() {
        // TODO:测试 generateToken / parse / isValid / isExpired
        // 提示:用 ReflectionTestUtils 设置 expireHours = -1L 模拟已过期 token
    }
}
