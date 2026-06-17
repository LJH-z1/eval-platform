package com.mavis.evalplatform.question.controller;

import com.mavis.evalplatform.common.result.PageResult;
import com.mavis.evalplatform.common.result.Result;
import com.mavis.evalplatform.question.entity.Question;
import com.mavis.evalplatform.question.service.QuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 问题 Controller — 接口契约
 * <p>
 * 由【向锏楠 FR-03】实现。
 *
 * @author 向锏楠
 */
@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
@Tag(name = "问题管理", description = "FR-03 问题输入与管理(向锏楠)")
@SecurityRequirement(name = "bearerAuth")
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping
    public Result<PageResult<Question>> page(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type) {
        return Result.success(questionService.page(pageNum, pageSize, category, type));
    }

    @GetMapping("/library")
    public Result<List<Question>> library() {
        return Result.success(questionService.listMyLibrary(null));
    }

    @PostMapping
    public Result<Question> create(@RequestBody Question q) {
        return Result.success("创建成功", questionService.create(q));
    }

    @PutMapping("/{id}")
    public Result<Question> update(@PathVariable Long id, @RequestBody Question q) {
        return Result.success(questionService.update(id, q));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        questionService.delete(id);
        return Result.success();
    }

    @PostMapping("/import")
    public Result<QuestionService.ImportResult> importBatch(@RequestParam("file") MultipartFile file) {
        return Result.success(questionService.importBatch(file));
    }
}
