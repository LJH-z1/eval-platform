package com.mavis.evalplatform.billing.controller;

import com.mavis.evalplatform.billing.service.BillingService;
import com.mavis.evalplatform.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 成本统计 Controller — FR-07
 *
 * @author 梁倩倩
 */
@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
@Tag(name = "成本统计", description = "FR-07 成本与耗时")
public class BillingController {

    private final BillingService billingService;

    @GetMapping("/summary")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "某评测的成本总览")
    public Result<Map<String, Object>> summary(@RequestParam Long evaluationId) {
        return Result.success(billingService.summary(evaluationId));
    }

    @GetMapping("/time-series")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "时段折线图(按 hour / day)")
    public Result<Map<String, Object>> timeSeries(
            @RequestParam Long evaluationId,
            @RequestParam(defaultValue = "hour") String granularity) {
        return Result.success(billingService.timeSeries(evaluationId, granularity));
    }

    @GetMapping("/by-model")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "各模型成本对比")
    public Result<List<Map<String, Object>>> byModel(@RequestParam Long evaluationId) {
        return Result.success(billingService.byModel(evaluationId));
    }

    @GetMapping("/platform-summary")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "平台级总成本统计")
    public Result<Map<String, Object>> platformSummary() {
        return Result.success(billingService.platformSummary());
    }

    @GetMapping("/export")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "导出某评测的 CSV")
    public ResponseEntity<byte[]> exportCsv(@RequestParam Long evaluationId) {
        byte[] data = billingService.exportCsv(evaluationId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=utf-8"));
        headers.setContentDispositionFormData("attachment",
                "billing_eval_" + evaluationId + ".csv");
        return new ResponseEntity<>(data, headers, 200);
    }
}
