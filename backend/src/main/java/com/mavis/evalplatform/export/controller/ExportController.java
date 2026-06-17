package com.mavis.evalplatform.export.controller;

import com.mavis.evalplatform.common.result.Result;
import com.mavis.evalplatform.export.service.ReportExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 报告导出 Controller — 接口契约
 * <p>
 * 对齐架构设计说明书 §6.2.6
 * <p>
 * 由【周文泽 FR-08】实现。
 *
 * @author 周文泽
 */
@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
@Tag(name = "报告导出", description = "FR-08 报告导出与自动化测试(周文泽)")
@SecurityRequirement(name = "bearerAuth")
public class ExportController {

    private final ReportExportService reportService;

    @GetMapping(value = "/{evaluationId}/excel",
                produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> excel(@PathVariable Long evaluationId) {
        ByteArrayOutputStream out = reportService.exportExcel(evaluationId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=eval_report_" + evaluationId + ".xlsx")
                .body(out.toByteArray());
    }

    @GetMapping(value = "/{evaluationId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf(@PathVariable Long evaluationId) {
        ByteArrayOutputStream out = reportService.exportPdf(evaluationId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=eval_report_" + evaluationId + ".pdf")
                .body(out.toByteArray());
    }
}
