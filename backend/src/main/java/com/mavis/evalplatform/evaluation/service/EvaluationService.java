package com.mavis.evalplatform.evaluation.service;

import com.mavis.evalplatform.common.result.PageResult;
import com.mavis.evalplatform.evaluation.entity.Answer;
import com.mavis.evalplatform.evaluation.entity.Evaluation;

import java.util.List;

/**
 * 评测 Service — 接口契约
 * <p>
 * 由【梁倩倩 FR-04】实现。
 * <p>
 * 业务流程(对齐需求规格说明书 §3.4.3):
 * <ol>
 *   <li>用户提交问题 + 模型列表 → 创建 Evaluation(PENDING)</li>
 *   <li>启动 /run → EvaluationRunner 并行调用多个 ModelAdapter</li>
 *   <li>逐个完成 → 写 answer 表;失败也写(errorCode/errorMessage)</li>
 *   <li>全部完成 → 状态改 COMPLETED,推送 SSE done=true</li>
 *   <li>前端并排展示 + 差异高亮</li>
 * </ol>
 * <p>
 * 并行实现关键点:
 * <ul>
 *   <li>用 {@code @Async("evaluationExecutor")} + {@code CompletableFuture}</li>
 *   <li>单模型失败 catch 后写 error_answer,不影响其他模型</li>
 *   <li>至少 2 个模型才能启动(§3.4.4)</li>
 *   <li>单次评测问题数 ≤ 50,总耗时 ≤ 10 分钟</li>
 * </ul>
 *
 * @author 梁倩倩
 */
public interface EvaluationService {

    /** 创建评测(PENDING) */
    Evaluation create(String name, String description, List<Long> modelIds, List<Long> questionIds, Long userId);

    /** 启动评测(RUNNING) */
    void start(Long evaluationId);

    /** 查询详情 */
    Evaluation getById(Long id);

    /** 列表/分页 */
    PageResult<Evaluation> page(long pageNum, long pageSize, Long creatorId, String status);

    /** 获取某评测的所有 answer(并排展示用) */
    List<Answer> listAnswers(Long evaluationId);
}
