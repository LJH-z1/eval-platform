package com.mavis.evalplatform;

import com.mavis.evalplatform.auth.filter.JwtAuthenticationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 架构骨架冒烟测试 — 验证关键基础类加载正常
 * <p>
 * 不依赖任何业务实现(因为大部分 Service 都抛 TODO),
 * 只验证:
 * <ul>
 *   <li>关键枚举(角色)有正确的 code/description</li>
 *   <li>Auth 包的重要 record / 内部类(JwtAuthenticationFilter.AuthenticatedUser)工作正常</li>
 * </ul>
 *
 * @author 刘家豪
 */
@DisplayName("架构骨架冒烟测试")
class SkeletonSmokeTest {

    @Test
    @DisplayName("Role 枚举值正确")
    void role_enum_ok() {
        assertEquals("ADMIN", com.mavis.evalplatform.auth.entity.Role.ADMIN.getCode());
        assertEquals("系统管理员", com.mavis.evalplatform.auth.entity.Role.ADMIN.getDescription());
        assertEquals("SCORER", com.mavis.evalplatform.auth.entity.Role.SCORER.getCode());
        assertEquals("评分员", com.mavis.evalplatform.auth.entity.Role.SCORER.getDescription());
        assertTrue(com.mavis.evalplatform.auth.entity.Role.of(null) == com.mavis.evalplatform.auth.entity.Role.VISITOR);
    }

    @Test
    @DisplayName("AuthenticatedUser record 工作正常")
    void authenticated_user_record_ok() {
        JwtAuthenticationFilter.AuthenticatedUser u = new JwtAuthenticationFilter.AuthenticatedUser(1L, "admin", "ADMIN");
        assertEquals(1L, u.id());
        assertEquals("admin", u.username());
        assertEquals("ADMIN", u.role());
        assertTrue(u.isAdmin());
    }
}
