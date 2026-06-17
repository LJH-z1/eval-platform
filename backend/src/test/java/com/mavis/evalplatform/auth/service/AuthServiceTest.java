package com.mavis.evalplatform.auth.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * AuthService 单元测试 — 骨架
 * <p>
 * 完整测试由【刘家豪 FR-01】实现,覆盖对齐测试计划文档 §6.1 的 TC-01-001 ~ TC-01-006。
 * <p>
 * 推荐使用 Mockito + @InjectMocks,见 {@code eval-platform-reference} 参考实现。
 * <p>
 * 必测场景:
 * <ul>
 *   <li>TC-01-001:正常登录 → 返回 token + userInfo</li>
 *   <li>TC-01-002:密码错误 → 1003 PASSWORD_INCORRECT</li>
 *   <li>TC-01-003:连续 5 次失败 → 1005 USER_LOCKED + 锁定 30 分钟</li>
 *   <li>TC-01-004:Token 过期 → 401</li>
 *   <li>TC-01-006:密码以 BCrypt 形式存储</li>
 * </ul>
 *
 * @author 刘家豪
 */
@DisplayName("AuthService 单元测试(骨架)")
class AuthServiceTest {

    @Test
    @DisplayName("TODO 由刘家豪实现:TC-01-001 ~ 006")
    void placeholder() {
        // TODO:用 @Mock UserMapper / @InjectMocks AuthService,验证
        // 1) login 成功路径
        // 2) 密码错误抛 BusinessException(1003)
        // 3) 5 次失败锁定(1005)
        // 4) BCrypt 加密存储
    }
}
