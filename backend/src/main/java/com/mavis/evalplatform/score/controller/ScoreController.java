package com.mavis.evalplatform.score.controller;

import com.mavis.evalplatform.common.result.Result;
import com.mavis.evalplatform.score.entity.Score;
import com.mavis.evalplatform.score.service.ScoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.mavis.evalplatform.auth.filter.JwtAuthenticationFilter.AuthenticatedUser;

/**
 * 评分 Controller — 接口契约
 * <p>
 * 对齐架构设计说明书 §6.2.4 + 需求规格说明书 §3.5
 * <p>
 * 由【宋子翔 FR-05】实现。
 *
 * @author 宋子翔
 */
@RestController
@RequestMapping("/api/scores")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER', 'SCORER')")
@Tag(name = "评分", description = "FR-05 多维度人工评分(宋子翔)")
@SecurityRequirement(name = "bearerAuth")
public class ScoreController {

    private final ScoreService scoreService;

    @PostMapping
    public Result<Score> submit(@AuthenticationPrincipal AuthenticatedUser user,
                                @Valid @RequestBody SubmitRequest req) {
        return Result.success("评分已提交,不可修改", scoreService.submit(
                req.getAnswerId(), user.id(),
                req.getAccuracy(), req.getRelevance(), req.getFluency(), req.getSafety(),
                req.getComment()));
    }

    @GetMapping("/by-evaluation")
    public Result<List<Score>> listByEvaluation(@RequestParam Long evaluationId,
                                                @AuthenticationPrincipal AuthenticatedUser user) {
        return Result.success(scoreService.listByScorerAndEvaluation(user.id(), evaluationId));
    }

    @Data
    public static class SubmitRequest {
        @NotNull
        private Long answerId;

        @NotNull @Min(1) @Max(5) private Integer accuracy;
        @NotNull @Min(1) @Max(5) private Integer relevance;
        @NotNull @Min(1) @Max(5) private Integer fluency;
        @NotNull @Min(1) @Max(5) private Integer safety;

        @Size(max = 500)
        private String comment;
    }
}
