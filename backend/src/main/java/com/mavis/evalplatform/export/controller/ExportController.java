package com.mavis.evalplatform.export.controller;

import com.mavis.evalplatform.common.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 占位 — FR-08 由周文泽实现
 *
 * @author 周文泽
 */
@RestController
@RequestMapping("/api/export")
@Tag(name = "报告导出", description = "FR-08 由周文泽实现")
public class ExportController {

    @GetMapping("/{evaluationId}/excel")
    public Result<Map<String, Object>> excel(@PathVariable Long evaluationId) {
        return Result.error(501, "FR-08 待周文泽实现");
    }

    @GetMapping("/{evaluationId}/pdf")
    public Result<Map<String, Object>> pdf(@PathVariable Long evaluationId) {
        return Result.error(501, "FR-08 待周文泽实现");
    }
}
