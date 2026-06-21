package com.mavis.evalplatform.export.service;

import java.io.ByteArrayOutputStream;

/**
 * 报告导出 Service — 接口契约(FR-08)
 * <p>
 * 对齐架构设计说明书 §4.2.8 + 需求规格说明书 §3.8
 * <p>
 * 由【周文泽 FR-08】实现。
 * <p>
 * 报告内容(6 章):
 * <ol>
 *   <li>封面(评测名/创建者/创建时间/模型数/问题数)</li>
 *   <li>评测概述</li>
 *   <li>模型调用统计(耗时/Token/费用)</li>
 *   <li>评分统计(各模型各维度平均分)</li>
 *   <li>评分一致性分析(Kappa)</li>
 *   <li>详细结果(问题 × 模型 × 评分表)</li>
 * </ol>
 * <p>
 * 简化实现:Excel 用多 Sheet 的 CSV 格式(Excel 可直接打开),
 * PDF 用 HTML 格式(浏览器可查看,也可打印为 PDF)。
 *
 * @author 周文泽
 */
public interface ReportExportService {

    /** 导出 CSV(可被 Excel 打开) */
    ByteArrayOutputStream exportCsv(Long evaluationId);

    /** 导出 HTML(可被浏览器/Word 打开) */
    ByteArrayOutputStream exportHtml(Long evaluationId);

    /** 报告元数据(用于前端预览) */
    Object reportMeta(Long evaluationId);
}
