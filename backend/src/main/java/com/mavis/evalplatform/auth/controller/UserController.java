package com.mavis.evalplatform.auth.controller;

import com.mavis.evalplatform.auth.dto.UserInfo;
import com.mavis.evalplatform.auth.service.UserService;
import com.mavis.evalplatform.common.result.PageResult;
import com.mavis.evalplatform.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理 Controller(管理员)
 * <p>
 * 简单实现:分页查询 + 禁用(对齐 FR-01 用户管理)
 *
 * @author 刘家豪
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "用户管理", description = "仅管理员可访问")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @Operation(summary = "分页查询用户")
    @GetMapping
    public Result<PageResult<UserInfo>> page(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String role) {
        return Result.success(userService.page(pageNum, pageSize, role));
    }

    @Operation(summary = "禁用用户")
    @PostMapping("/{id}/disable")
    public Result<Void> disable(@PathVariable Long id) {
        userService.disableUser(id);
        return Result.success();
    }

    @Operation(summary = "启用用户")
    @PostMapping("/{id}/enable")
    public Result<Void> enable(@PathVariable Long id) {
        userService.enableUser(id);
        return Result.success();
    }
}
