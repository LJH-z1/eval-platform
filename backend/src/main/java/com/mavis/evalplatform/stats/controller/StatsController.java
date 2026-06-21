package com.mavis.evalplatform.stats.controller;

import com.mavis.evalplatform.common.result.Result;
import com.mavis.evalplatform.stats.service.FleissKappaService;
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
 * 一致性分析 Controller — FR-06
 *
 * @author 宋子翔
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
@Tag(name = "一致性分析", description = "FR-06 Fleiss Kappa + 排名")
public class StatsController {

    private final FleissKappaService fleissKappaService;

    @GetMapping("/kappa")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取某评测的 Fleiss Kappa(4 维度)")
    public Result<Map<String, Object>> getKappa(@RequestParam Long evaluationId) {
        return Result.success(fleissKappaService.getKappa(evaluationId));
    }

    @GetMapping("/controversial")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "争议项 — 同问题评分标准差 > 1.5")
    public Result<List<Map<String, Object>>> getControversial(@RequestParam Long evaluationId) {
        return Result.success(fleissKappaService.getControversialItems(evaluationId));
    }

    @GetMapping("/scorer-ranking")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "评分员排行 — 已评数 / 平均分 / 覆盖率")
    public Result<List<Map<String, Object>>> getScorerRanking(@RequestParam Long evaluationId) {
        return Result.success(fleissKappaService.getScorerRanking(evaluationId));
    }

    @GetMapping("/model-ranking")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "模型排名 — 4 维平均分 + 加权总分(0.4/0.3/0.2/0.1)")
    public Result<List<Map<String, Object>>> getModelRanking(@RequestParam Long evaluationId) {
        return Result.success(fleissKappaService.getModelRanking(evaluationId));
    }
}
