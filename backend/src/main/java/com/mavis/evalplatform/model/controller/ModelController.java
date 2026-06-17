package com.mavis.evalplatform.model.controller;

import com.mavis.evalplatform.common.result.PageResult;
import com.mavis.evalplatform.common.result.Result;
import com.mavis.evalplatform.model.entity.ModelConfig;
import com.mavis.evalplatform.model.service.ModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模型配置 Controller — 接口契约
 * <p>
 * 对齐架构设计说明书 §6.2.2 + 需求规格说明书 §3.2
 * <p>
 * 由【向锏楠 FR-02】实现具体业务。
 *
 * @author 向锏楠
 */
@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
@Tag(name = "模型配置", description = "FR-02 模型配置管理(向锏楠)")
@SecurityRequirement(name = "bearerAuth")
public class ModelController {

    private final ModelService modelService;

    @Operation(summary = "分页查询")
    @GetMapping
    public Result<PageResult<ModelConfig>> page(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String provider) {
        return Result.success(modelService.page(pageNum, pageSize, provider));
    }

    @Operation(summary = "已启用模型列表(供评测下拉用)")
    @GetMapping("/enabled")
    public Result<List<ModelConfig>> listEnabled() {
        return Result.success(modelService.listEnabled());
    }

    @Operation(summary = "新增模型")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<ModelConfig> create(@RequestBody ModelConfig model) {
        return Result.success("创建成功", modelService.create(model));
    }

    @Operation(summary = "更新模型")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<ModelConfig> update(@PathVariable Long id, @RequestBody ModelConfig model) {
        return Result.success(modelService.update(id, model));
    }

    @Operation(summary = "启用/停用")
    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> toggle(@PathVariable Long id, @RequestParam int status) {
        modelService.toggleStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "删除(被引用时拒绝)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        modelService.delete(id);
        return Result.success();
    }

    @Operation(summary = "连接测试")
    @PostMapping("/test")
    public Result<ModelService.ModelTestResult> test(@RequestParam Long id, @RequestParam String testQuestion) {
        return Result.success(modelService.test(id, testQuestion));
    }
}
