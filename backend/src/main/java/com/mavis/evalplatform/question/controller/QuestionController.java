package com.mavis.evalplatform.question.controller;

import com.mavis.evalplatform.auth.filter.JwtAuthenticationFilter.AuthenticatedUser;
import com.mavis.evalplatform.common.exception.BusinessException;
import com.mavis.evalplatform.common.exception.ErrorCode;
import com.mavis.evalplatform.common.result.PageResult;
import com.mavis.evalplatform.common.result.Result;
import com.mavis.evalplatform.question.dto.QuestionRequest;
import com.mavis.evalplatform.question.dto.QuestionVO;
import com.mavis.evalplatform.question.entity.Question;
import com.mavis.evalplatform.question.service.QuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 问题管理 Controller — FR-03 完整实现
 * <p>
 * 对齐架构设计说明书 §6.2.3 + 需求规格说明书 §3.3。
 *
 * @author 向锏楠
 */
@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER', 'SCORER')")
@Tag(name = "问题管理", description = "FR-03 问题输入与管理(向锏楠 ✅)")
@SecurityRequirement(name = "bearerAuth")
public class QuestionController {

    private final QuestionService questionService;

    @Operation(summary = "分页查询(支持 category/type/difficulty/keyword)")
    @GetMapping
    public Result<PageResult<QuestionVO>> page(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String keyword) {
        PageResult<Question> p = questionService.page(pageNum, pageSize, category, type, difficulty, keyword);
        return Result.success(PageResult.of(
                p.getList().stream().map(QuestionVO::from).toList(),
                p.getTotal(), p.getPageNum(), p.getPageSize()));
    }

    @Operation(summary = "题库列表(评测页/评分页下拉用)")
    @GetMapping("/library")
    public Result<List<QuestionVO>> library(@AuthenticationPrincipal AuthenticatedUser user) {
        List<Question> list = questionService.listForLibrary(user == null ? null : user.id());
        return Result.success(list.stream().map(QuestionVO::from).toList());
    }

    @Operation(summary = "查看详情")
    @GetMapping("/{id}")
    public Result<QuestionVO> get(@PathVariable Long id) {
        return Result.success(QuestionVO.from(questionService.getById(id)));
    }

    @Operation(summary = "新建问题(单题输入)")
    @PostMapping
    public Result<QuestionVO> create(@AuthenticationPrincipal AuthenticatedUser user,
                                     @Valid @RequestBody QuestionRequest req) {
        Long uid = user == null ? null : user.id();
        return Result.success("创建成功",
                QuestionVO.from(questionService.create(req, uid)));
    }

    @Operation(summary = "更新问题")
    @PutMapping("/{id}")
    public Result<QuestionVO> update(@PathVariable Long id,
                                     @Valid @RequestBody QuestionRequest req) {
        return Result.success("更新成功",
                QuestionVO.from(questionService.update(id, req)));
    }

    @Operation(summary = "软删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        questionService.delete(id);
        return Result.success();
    }

    @Operation(summary = "批量导入(CSV,每行 1 题,≤ 200 题/次)")
    @PostMapping("/import")
    public Result<QuestionService.ImportResult> importBatch(@RequestParam("file") MultipartFile file) {
        if (userIsNull()) {
            // 拦截器已处理,这里走不到
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return Result.success(questionService.importBatch(file));
    }

    private boolean userIsNull() {
        return SecurityContextUser() == null;
    }

    private AuthenticatedUser SecurityContextUser() {
        org.springframework.security.core.Authentication a =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (a == null) return null;
        Object p = a.getPrincipal();
        return p instanceof AuthenticatedUser ? (AuthenticatedUser) p : null;
    }
}
