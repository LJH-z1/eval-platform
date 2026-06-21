package com.mavis.evalplatform.stats.service;

import java.util.List;
import java.util.Map;

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
 *
 * @author 宋子翔
 */
public interface FleissKappaService {

    /**
     * 核心算法 — 计算 Fleiss Kappa 系数
     */
    double calculate(List<List<Integer>> scores, int kCategories);

    /**
     * 业务接口:获取某评测的 4 维度 Kappa + 评分员数 + 总评分数
     */
    Map<String, Object> getKappa(Long evaluationId);

    /**
     * 识别争议项(同问题评分标准差 > 1.5)
     */
    List<Map<String, Object>> getControversialItems(Long evaluationId);

    /**
     * 评分员排行(已评数、平均分、覆盖率)
     */
    List<Map<String, Object>> getScorerRanking(Long evaluationId);

    /**
     * 模型排名(4 维平均分 + 加权总分)
     */
    List<Map<String, Object>> getModelRanking(Long evaluationId);
}
