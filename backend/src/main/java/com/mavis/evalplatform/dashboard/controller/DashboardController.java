package com.mavis.evalplatform.dashboard.controller;

import com.mavis.evalplatform.common.result.Result;
import com.mavis.evalplatform.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 首页 Dashboard Controller — 兼容旧版前端 + 提供总览数据
 *
 * @author 刘家豪
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "首页总览", description = "Dashboard 聚合统计")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "总览统计")
    public Result<Map<String, Object>> stats() {
        return Result.success(dashboardService.stats());
    }

    @GetMapping("/recent")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "最近评测")
    public Result<List<Map<String, Object>>> recent(@RequestParam(defaultValue = "10") int limit) {
        return Result.success(dashboardService.recent(limit));
    }
}
