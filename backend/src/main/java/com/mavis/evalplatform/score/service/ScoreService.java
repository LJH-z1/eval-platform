package com.mavis.evalplatform.score.service;

import com.mavis.evalplatform.score.entity.Score;

import java.util.List;

/**
 * 评分 Service — 接口契约
 * <p>
 * 由【宋子翔 FR-05】实现。
 * <p>
 * 业务规则(对齐需求规格说明书 §3.5.6):
 * <ul>
 *   <li>每评分员对每个回答只能评 1 次(UNIQUE 约束)</li>
 *   <li>提交后不允许修改(防误操作)</li>
 *   <li>评语 ≤ 500 字</li>
 * </ul>
 *
 * @author 宋子翔
 */
public interface ScoreService {

    /**
     * 提交评分
     * @throws com.mavis.evalplatform.common.exception.BusinessException 1002 已评过分
     */
    Score submit(Long answerId, Long scorerId, Integer accuracy, Integer relevance,
                 Integer fluency, Integer safety, String comment);

    /**
     * 获取某评分员在某评测的所有评分
     */
    List<Score> listByScorerAndEvaluation(Long scorerId, Long evaluationId);

    /**
     * 获取某回答的所有评分
     */
    List<Score> listByAnswer(Long answerId);
}
