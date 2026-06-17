package com.mavis.evalplatform.stats.controller;

import com.mavis.evalplatform.common.result.Result;
import com.mavis.evalplatform.stats.service.FleissKappaService;
import com.mavis.evalplatform.stats.service.FleissKappaService.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 统计 Controller — 接口契约
 * <p>
 * 对齐架构设计说明书 §6.2.5 + 需求规格说明书 §3.6
 * <p>
 * 由【宋子翔 FR-06】实现。
 *
 * @author 宋子翔
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER', 'SCORER')")
@Tag(name = "统计", description = "FR-06 评分一致性分析(宋子翔)")
@SecurityRequirement(name = "bearerAuth")
public class StatsController {

    private final FleissKappaService kappaService;

    @GetMapping("/kappa")
    public Result<KappaResult> kappa(@RequestParam Long evaluationId) {
        return Result.success(kappaService.getKappa(evaluationId));
    }

    @GetMapping("/controversial")
    public Result<List<ControversialItem>> controversial(@RequestParam Long evaluationId) {
        return Result.success(kappaService.getControversialItems(evaluationId));
    }

    @GetMapping("/scorer-ranking")
    public Result<List<ScorerRanking>> scorerRanking(@RequestParam Long evaluationId) {
        return Result.success(kappaService.getScorerRanking(evaluationId));
    }

    @GetMapping("/model-ranking")
    public Result<List<ModelRanking>> modelRanking(@RequestParam Long evaluationId) {
        return Result.success(kappaService.getModelRanking(evaluationId));
    }
}
