package com.mavis.evalplatform.stats.service;

import java.util.List;

/**
 * 一致性分析(Fleiss Kappa)Service — 接口契约
 * <p>
 * 对齐架构设计说明书 §4.2.6 + 需求规格说明书 §3.6
 * <p>
 * 由【宋子翔 FR-06】实现。
 * <p>
 * Fleiss Kappa 公式:
 * <pre>
 *   κ = (P̄ - P̄e) / (1 - P̄e)
 * </pre>
 * <p>
 * 解读:
 * <ul>
 *   <li>&lt; 0 比随机还差</li>
 *   <li>0.0-0.20 轻微</li>
 *   <li>0.21-0.40 一般</li>
 *   <li>0.41-0.60 中等</li>
 *   <li>0.61-0.80 良好</li>
 *   <li>0.81-1.00 优秀</li>
 * </ul>
 * <p>
 * 测试数据(对齐测试计划 §6.12):
 * <ul>
 *   <li>TC-KAPPA-001 完美一致 → 1.0</li>
 *   <li>TC-KAPPA-002 随机一致 → ≈ 0</li>
 *   <li>TC-KAPPA-003 Fleiss 1971 教科书示例 → 0.591</li>
 * </ul>
 *
 * @author 宋子翔
 */
public interface FleissKappaService {

    /**
     * 计算 Fleiss Kappa 系数
     * @param scores 二维数组 scores[i][j] = 第 i 个回答被第 j 个评分员打的分数(1-5)
     * @param kCategories 类别数(本项目固定为 5,即 1-5 分)
     * @return Kappa 值
     */
    double calculate(List<List<Integer>> scores, int kCategories);

    /** 解读 Kappa 值 */
    default String interpret(double kappa) {
        if (kappa < 0) return "比随机还差";
        if (kappa <= 0.20) return "轻微一致性";
        if (kappa <= 0.40) return "一般一致性";
        if (kappa <= 0.60) return "中等一致性";
        if (kappa <= 0.80) return "良好一致性";
        return "优秀一致性";
    }

    /**
     * 业务接口:获取某评测的 Kappa(自动收集 score 数据)
     * <p>
     * 业务规则(§3.6.4):
     * <ul>
     *   <li>至少 3 名评分员、每回答 ≥ 2 个评分,否则提示"评分员不足"</li>
     *   <li>Redis 缓存 5 分钟,命中率 ≥ 80%</li>
     * </ul>
     */
    KappaResult getKappa(Long evaluationId);

    /**
     * 识别争议项(同问题评分标准差 > 1.5)
     */
    List<ControversialItem> getControversialItems(Long evaluationId);

    /**
     * 评分员排行(平均分、覆盖率、被申诉次数)
     */
    List<ScorerRanking> getScorerRanking(Long evaluationId);

    /**
     * 模型排名(各维度平均分 + 加权总分)
     */
    List<ModelRanking> getModelRanking(Long evaluationId);

    record KappaResult(double kappa, String interpretation, int scorerCount, int answerCount) {}
    record ControversialItem(Long questionId, double stdDev, List<Integer> scores) {}
    record ScorerRanking(Long scorerId, String username, double avgScore, double coverage, int appealed) {}
    record ModelRanking(Long modelId, String modelName, double accuracy, double relevance,
                       double fluency, double safety, double weighted, int rank) {}
}
