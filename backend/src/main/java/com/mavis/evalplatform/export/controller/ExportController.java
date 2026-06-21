package com.mavis.evalplatform.export.controller;

import com.mavis.evalplatform.common.exception.BusinessException;
import com.mavis.evalplatform.common.exception.ErrorCode;
import com.mavis.evalplatform.common.result.Result;
import com.mavis.evalplatform.export.service.ReportExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 报告导出 Controller — FR-08
 *
 * @author 周文泽
 */
@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
@Tag(name = "报告导出", description = "FR-08 评测报告(CSV / HTML)")
public class ExportController {

    private final ReportExportService reportExportService;

    @GetMapping("/{evaluationId}/excel")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "导出 Excel(实际是 CSV,Excel 可直接打开)")
    public ResponseEntity<byte[]> exportExcel(@PathVariable Long evaluationId) {
        ByteArrayOutputStream data = reportExportService.exportCsv(evaluationId);
        return download(data, "evaluation_" + evaluationId + ".csv", "text/csv; charset=utf-8");
    }

    @GetMapping("/{evaluationId}/pdf")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "导出 PDF(实际是 HTML,浏览器/Word 可打开)")
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long evaluationId) {
        ByteArrayOutputStream data = reportExportService.exportHtml(evaluationId);
        return download(data, "evaluation_" + evaluationId + ".html", "text/html; charset=utf-8");
    }

    @GetMapping("/{evaluationId}/meta")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取报告元数据(用于前端预览)")
    public Result<Object> meta(@PathVariable Long evaluationId) {
        try {
            return Result.success(reportExportService.reportMeta(evaluationId));
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        }
    }

    private ResponseEntity<byte[]> download(ByteArrayOutputStream data, String filename, String contentType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        try {
            String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8);
            headers.set("Content-Disposition", "attachment; filename=\"" + encoded + "\"");
        } catch (Exception ex) {
            headers.set("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        }
        return new ResponseEntity<>(data.toByteArray(), headers, 200);
    }
}
