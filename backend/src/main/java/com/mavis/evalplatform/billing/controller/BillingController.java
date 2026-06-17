package com.mavis.evalplatform.billing.controller;

import com.mavis.evalplatform.billing.service.BillingService;
import com.mavis.evalplatform.billing.service.BillingService.*;
import com.mavis.evalplatform.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 成本统计 Controller — 接口契约
 * <p>
 * 对齐架构设计说明书 §4.2.7 + 需求规格说明书 §3.7
 * <p>
 * 由【梁倩倩 FR-07】实现。
 *
 * @author 梁倩倩
 */
@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
@Tag(name = "成本统计", description = "FR-07 成本与耗时统计(梁倩倩)")
@SecurityRequirement(name = "bearerAuth")
public class BillingController {

    private final BillingService billingService;

    @GetMapping("/summary")
    public Result<CostStats> summary(@RequestParam Long evaluationId) {
        return Result.success(billingService.summary(evaluationId));
    }

    @GetMapping("/time-series")
    public Result<List<TimeSeriesData>> timeSeries(@RequestParam Long evaluationId,
                                                    @RequestParam(defaultValue = "hour") String granularity) {
        return Result.success(billingService.timeSeries(evaluationId, granularity));
    }

    @GetMapping("/by-model")
    public Result<List<ModelCost>> byModel(@RequestParam Long evaluationId) {
        return Result.success(billingService.byModel(evaluationId));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam Long evaluationId) {
        byte[] data = billingService.exportCsv(evaluationId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=billing_" + evaluationId + ".csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(data);
    }
}
