package com.mavis.evalplatform.export.service;

import java.io.ByteArrayOutputStream;

/**
 * 报告导出 Service — 接口契约
 * <p>
 * 对齐架构设计说明书 §4.2.8 + 需求规格说明书 §3.8
 * <p>
 * 由【周文泽 FR-08】实现。
 * <p>
 * 报告内容(6 章):
 * <ol>
 *   <li>封面(评测名/创建者/创建时间/模型数/问题数)</li>
 *   <li>目录(自动生成)</li>
 *   <li>第一章 评测概述</li>
 *   <li>第二章 模型调用统计(耗时/Token/费用)</li>
 *   <li>第三章 评分统计(各模型各维度平均分)</li>
 *   <li>第四章 评分一致性分析(Kappa)</li>
 *   <li>第五章 详细结果(问题 × 模型 × 评分表)</li>
 *   <li>第六章 结论</li>
 * </ol>
 * <p>
 * 文件名格式:评测报告_{名称}_{YYYYMMDD}.xlsx
 *
 * @author 周文泽
 */
public interface ReportExportService {

    /** Excel 导出(EasyExcel) */
    ByteArrayOutputStream exportExcel(Long evaluationId);

    /** PDF 导出(iText + Flying Saucer) */
    ByteArrayOutputStream exportPdf(Long evaluationId);
}
