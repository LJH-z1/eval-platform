package com.mavis.evalplatform.arena.controller;

import com.mavis.evalplatform.arena.service.ArenaService;
import com.mavis.evalplatform.auth.filter.JwtAuthenticationFilter.AuthenticatedUser;
import com.mavis.evalplatform.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Arena 盲评 Controller
 *
 * @author 刘家豪
 */
@RestController
@RequestMapping("/api/arena")
@RequiredArgsConstructor
@Tag(name = "Arena 盲评", description = "LMArena 风格盲评对比 + Elo 排名")
public class ArenaController {

    private final ArenaService arenaService;

    /** 快速评测 1 题 2 模型(同步) */
    @PostMapping("/quick-eval")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Arena 快速评测(1 题 2 模型,同步返回,body 含 category)")
    public Result<Map<String, Object>> quickEval(@RequestBody Map<String, Object> body,
                                                 @AuthenticationPrincipal AuthenticatedUser user) {
        String prompt = (String) body.get("prompt");
        Long a = toLong(body.get("modelAId"));
        Long b = toLong(body.get("modelBId"));
        String category = (String) body.get("category");
        return Result.success(arenaService.quickEvaluate(prompt, a, b, category, user.id()));
    }

    /** 批量评测 N 题 2 模型(同步,内部并发跑) */
    @PostMapping("/batch-eval")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Arena 批量评测(N 题 2 模型,并发跑,返回每题结果)")
    public Result<java.util.List<Map<String, Object>>> batchEval(@RequestBody Map<String, Object> body,
                                                                 @AuthenticationPrincipal AuthenticatedUser user) {
        @SuppressWarnings("unchecked")
        java.util.List<String> prompts = (java.util.List<String>) body.get("prompts");
        Long a = toLong(body.get("modelAId"));
        Long b = toLong(body.get("modelBId"));
        String category = (String) body.get("category");
        return Result.success(arenaService.batchEvaluate(prompts, a, b, category, user.id()));
    }

    /** 投票 */
    @PostMapping("/vote")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "投票(A/B/tie/bad,带 category)")
    public Result<Long> vote(@RequestBody Map<String, Object> body,
                             @AuthenticationPrincipal AuthenticatedUser user) {
        Long voteId = arenaService.vote(
                toLong(body.get("evaluationId")),
                (String) body.get("prompt"),
                toLong(body.get("leftModelId")),
                toLong(body.get("rightModelId")),
                (String) body.get("winner"),
                (String) body.get("category"),
                user.id());
        return Result.success(voteId);
    }

    /** Elo 排行榜 */
    @GetMapping("/ranking")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Elo 排行榜(可按能力分类 ?category=text/vision/code 等)")
    public Result<List<Map<String, Object>>> ranking(
            @RequestParam(required = false) String category) {
        return Result.success(arenaService.rankingByCategory(category));
    }

    private static Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        return Long.valueOf(o.toString());
    }
}
