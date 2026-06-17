package com.mavis.evalplatform.billing.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 成本与耗时统计 Service — 接口契约
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
     * 实时计时(每个 answer 已有 latency_ms,这里做汇总)
     */
    CostStats summary(Long evaluationId);

    /**
     * 按时段折线图(调用量 + 费用)
     */
    List<TimeSeriesData> timeSeries(Long evaluationId, String granularity);

    /**
     * 各模型对比(柱状图)
     */
    List<ModelCost> byModel(Long evaluationId);

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
