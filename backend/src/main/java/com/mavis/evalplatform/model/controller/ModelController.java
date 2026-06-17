package com.mavis.evalplatform.model.controller;

import com.mavis.evalplatform.common.result.PageResult;
import com.mavis.evalplatform.common.result.Result;
import com.mavis.evalplatform.model.dto.ModelConfigRequest;
import com.mavis.evalplatform.model.dto.ModelConfigVO;
import com.mavis.evalplatform.model.service.ModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 模型配置 Controller — FR-02
 *
 * @author 向锏楠
 */
@Slf4j
@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
@Tag(name = "模型配置", description = "FR-02 模型配置管理")
public class ModelController {

    private final ModelService modelService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    @Operation(summary = "分页查询模型列表")
    public Result<PageResult<ModelConfigVO>> page(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String provider) {
        return Result.success(modelService.page(pageNum, pageSize, provider));
    }

    @GetMapping("/enabled")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER', 'SCORER')")
    @Operation(summary = "查询所有已启用模型(评测下拉)")
    public Result<List<ModelConfigVO>> listEnabled() {
        return Result.success(modelService.listEnabled());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取模型详情(apiKey 掩码)")
    public Result<ModelConfigVO> getById(@PathVariable Long id) {
        return Result.success(modelService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "创建模型(API Key 自动加密)")
    public Result<ModelConfigVO> create(@RequestBody @Valid ModelConfigRequest req) {
        return Result.success("创建成功", modelService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "更新模型(provider 不允许改)")
    public Result<ModelConfigVO> update(@PathVariable Long id, @RequestBody @Valid ModelConfigRequest req) {
        return Result.success("更新成功", modelService.update(id, req));
    }

    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "启用/停用模型")
    public Result<Void> toggle(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Integer status = body == null ? null : body.get("status");
        if (status == null || (status != 0 && status != 1)) {
            return Result.error(400, "status 必须为 0 或 1");
        }
        modelService.toggleStatus(id, status);
        return Result.success("已" + (status == 1 ? "启用" : "停用"), null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "删除模型(被引用则 1023)")
    public Result<Void> delete(@PathVariable Long id) {
        modelService.delete(id);
        return Result.success("删除成功", null);
    }

    @PostMapping("/test")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    @Operation(summary = "连接测试(可临时配置或基于已存在模型)")
    public Result<ModelService.ModelTestResult> test(@RequestBody Map<String, Object> body) {
        String testQuestion = body.get("question") == null ? "你好" : body.get("question").toString();
        if (body.get("id") != null) {
            Long id = Long.valueOf(body.get("id").toString());
            return Result.success(modelService.test(id, testQuestion));
        }
        // 临时测试
        ModelConfigRequest req = new ModelConfigRequest();
        req.setProvider((String) body.get("provider"));
        req.setApiKey((String) body.get("apiKey"));
        req.setEndpoint((String) body.get("endpoint"));
        req.setModelVersion((String) body.get("modelVersion"));
        req.setTemperature(body.get("temperature") == null ? null
                : new java.math.BigDecimal(body.get("temperature").toString()));
        req.setTopP(body.get("topP") == null ? null
                : new java.math.BigDecimal(body.get("topP").toString()));
        req.setMaxTokens(body.get("maxTokens") == null ? null
                : Integer.valueOf(body.get("maxTokens").toString()));
        return Result.success(modelService.test(req, testQuestion));
    }
}
