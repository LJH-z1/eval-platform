package com.mavis.evalplatform.auth.controller;

import com.mavis.evalplatform.auth.dto.LoginRequest;
import com.mavis.evalplatform.auth.dto.LoginResponse;
import com.mavis.evalplatform.auth.dto.RegisterRequest;
import com.mavis.evalplatform.auth.dto.UserInfo;
import com.mavis.evalplatform.auth.filter.JwtAuthenticationFilter.AuthenticatedUser;
import com.mavis.evalplatform.auth.service.AuthService;
import com.mavis.evalplatform.auth.service.UserService;
import com.mavis.evalplatform.common.annotation.RateLimit;
import com.mavis.evalplatform.common.exception.BusinessException;
import com.mavis.evalplatform.common.exception.ErrorCode;
import com.mavis.evalplatform.common.result.Result;
import com.mavis.evalplatform.common.util.WebUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 认证 Controller
 * <p>
 * 对齐架构设计说明书 §6.2.1
 * - POST /api/auth/login
 * - POST /api/auth/logout
 * - GET  /api/auth/me
 * - POST /api/auth/register   (仅管理员,创建用户)
 * - POST /api/auth/change-password
 *
 * @author 刘家豪
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "FR-01 用户登录与权限管理(刘家豪)")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    /**
     * TC-01-001 / TC-01-002 / TC-01-003
     */
    @Operation(summary = "登录(返回 JWT,8 小时有效)")
    @PostMapping("/login")
    @RateLimit(limitPerMinute = 30, key = "auth.login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return Result.success(authService.login(req, WebUtil.getClientIp()));
    }

    /**
     * 注销
     */
    @Operation(summary = "注销", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/logout")
    public Result<Void> logout(@AuthenticationPrincipal AuthenticatedUser user) {
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        authService.logout(user.id(), user.username());
        return Result.success();
    }

    /**
     * 当前用户信息
     */
    @Operation(summary = "当前登录用户信息", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/me")
    public Result<UserInfo> me(@AuthenticationPrincipal AuthenticatedUser user) {
        if (user == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        return Result.success(userService.toUserInfo(userService.findById(user.id())));
    }

    /**
     * 创建账号
     * <p>
     * 开放注册(无需鉴权),但角色限制:
     * - 只能注册 SCORER / VISITOR
     * - 想注册 ADMIN/ORGANIZER 需现有 ADMIN 调用 /api/auth/register-admin(后端再加)
     */
    @Operation(summary = "注册用户(对外开放,只允许 SCORER/VISITOR)")
    @PostMapping("/register")
    public Result<UserInfo> register(@Valid @RequestBody RegisterRequest req) {
        String role = req.getRole() == null ? "SCORER" : req.getRole().toUpperCase();
        if (!"SCORER".equals(role) && !"VISITOR".equals(role)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID,
                "开放注册仅支持 SCORER / VISITOR 角色");
        }
        req.setRole(role);
        return Result.success("注册成功,请登录", userService.createUser(req));
    }

    /**
     * 管理员创建用户(任何角色)
     */
    @Operation(summary = "管理员创建用户(任意角色,需 ADMIN 登录)", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/register-admin")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<UserInfo> registerAdmin(@Valid @RequestBody RegisterRequest req) {
        return Result.success("创建成功", userService.createUser(req));
    }

    /**
     * 修改自己的密码
     */
    @Operation(summary = "修改密码", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/change-password")
    public Result<Void> changePassword(@AuthenticationPrincipal AuthenticatedUser user,
                                        @Valid @RequestBody com.mavis.evalplatform.auth.dto.ChangePasswordRequest req) {
        if (user == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        userService.changePassword(user.id(), req.getOldPassword(), req.getNewPassword());
        return Result.success();
    }
}
