package com.mavis.evalplatform.evaluation.controller;

import com.mavis.evalplatform.common.result.PageResult;
import com.mavis.evalplatform.common.result.Result;
import com.mavis.evalplatform.evaluation.entity.Answer;
import com.mavis.evalplatform.evaluation.entity.Evaluation;
import com.mavis.evalplatform.evaluation.service.EvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 评测 Controller — 接口契约
 * <p>
 * 对齐架构设计说明书 §6.2.3 + 需求规格说明书 §3.4
 * <p>
 * 由【梁倩倩 FR-04】实现。
 *
 * @author 梁倩倩
 */
@RestController
@RequestMapping("/api/evaluations")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
@Tag(name = "评测", description = "FR-04 多模型调用与对比展示(梁倩倩)")
@SecurityRequirement(name = "bearerAuth")
public class EvaluationController {

    private final EvaluationService evaluationService;

    @PostMapping
    public Result<Evaluation> create(@RequestBody CreateRequest req) {
        return Result.success("创建成功", evaluationService.create(
                req.name(), req.description(), req.modelIds(), req.questionIds(), req.userId()));
    }

    @PostMapping("/{id}/run")
    public Result<Void> run(@PathVariable Long id) {
        evaluationService.start(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<Evaluation> get(@PathVariable Long id) {
        return Result.success(evaluationService.getById(id));
    }

    @GetMapping
    public Result<PageResult<Evaluation>> page(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long creatorId,
            @RequestParam(required = false) String status) {
        return Result.success(evaluationService.page(pageNum, pageSize, creatorId, status));
    }

    @GetMapping("/{id}/answers")
    public Result<List<Answer>> answers(@PathVariable Long id) {
        return Result.success(evaluationService.listAnswers(id));
    }

    /**
     * SSE 流式输出(对齐需求规格说明书 §3.4.03)
     * <p>
     * data: {"questionId":1,"modelId":1,"chunk":"你好","done":false}
     * data: {"questionId":1,"modelId":1,"chunk":"！","done":true}
     */
    @GetMapping(value = "/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@PathVariable Long id) {
        throw new UnsupportedOperationException("TODO 梁倩倩 FR-04 实现 SSE 流式推送");
    }

    /** 创建请求体 */
    public record CreateRequest(String name, String description,
                                List<Long> modelIds, List<Long> questionIds, Long userId) {}
}
