package com.mavis.evalplatform.score.controller;

import com.mavis.evalplatform.auth.filter.JwtAuthenticationFilter.AuthenticatedUser;
import com.mavis.evalplatform.common.result.Result;
import com.mavis.evalplatform.score.dto.ScoreRequest;
import com.mavis.evalplatform.score.entity.Score;
import com.mavis.evalplatform.score.service.ScoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评分 Controller — FR-05
 *
 * @author 宋子翔
 */
@Slf4j
@RestController
@RequestMapping("/api/scores")
@RequiredArgsConstructor
@Tag(name = "评分", description = "FR-05 多维评分")
public class ScoreController {

    private final ScoreService scoreService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SCORER', 'ADMIN', 'ORGANIZER')")
    @Operation(summary = "提交评分(同一评分员对同一回答只能评 1 次)")
    public Result<Score> submit(@RequestBody @Valid ScoreRequest req,
                                @AuthenticationPrincipal AuthenticatedUser user) {
        Long scorerId = user == null ? null : user.id();
        if (scorerId == null) {
            return Result.error(401, "无法识别当前用户");
        }
        Score s = scoreService.submit(req, scorerId);
        return Result.success("评分已提交", s);
    }

    @GetMapping("/by-evaluation")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取当前评分员在某评测的所有评分(用于进度显示)")
    public Result<List<Score>> listByEvaluation(
            @RequestParam Long evaluationId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        Long scorerId = user == null ? null : user.id();
        if (scorerId == null) {
            return Result.error(401, "无法识别当前用户");
        }
        return Result.success(scoreService.listByScorerAndEvaluation(scorerId, evaluationId));
    }

    @GetMapping("/by-answer/{answerId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取某回答的所有评分(用于评分汇总)")
    public Result<List<Score>> listByAnswer(@PathVariable Long answerId) {
        return Result.success(scoreService.listByAnswer(answerId));
    }

    @GetMapping("/check")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "检查当前用户对某回答是否已评")
    public Result<Score> check(
            @RequestParam Long answerId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        Long scorerId = user == null ? null : user.id();
        if (scorerId == null) {
            return Result.success(null);
        }
        Score s = scoreService.findByAnswerAndScorer(answerId, scorerId);
        return Result.success(s);
    }
}
