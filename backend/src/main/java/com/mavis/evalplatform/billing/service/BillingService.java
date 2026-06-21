package com.mavis.evalplatform.billing.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 成本与耗时统计 Service — 接口契约(FR-07)
 * <p>
 * 对齐架构设计说明书 §4.2.7 + 需求规格说明书 §3.7
 * <p>
 * 由【梁倩倩 FR-07】实现。
 * <p>
 * 数据源:answer 表的 token_input / token_output / latency_ms / estimated_cost
 *
 * @author 梁倩倩
 */
public interface BillingService {

    /**
     * 某评测的总览
     */
    Map<String, Object> summary(Long evaluationId);

    /**
     * 按时段折线图(按 hour / day 粒度)
     */
    Map<String, Object> timeSeries(Long evaluationId, String granularity);

    /**
     * 各模型成本对比(柱状图)
     */
    List<Map<String, Object>> byModel(Long evaluationId);

    /**
     * 跨评测总览(看平台总成本)
     */
    Map<String, Object> platformSummary();

    /**
     * 导出 CSV
     */
    byte[] exportCsv(Long evaluationId);

    record CostStats(long totalCalls, long totalInputTokens, long totalOutputTokens,
                     BigDecimal totalCost, double avgLatencyMs) {}
    record TimeSeriesData(LocalDateTime time, long calls, BigDecimal cost) {}
    record ModelCost(Long modelId, String modelName, long calls, long tokens,
                     BigDecimal cost, double avgLatencyMs) {}
}
