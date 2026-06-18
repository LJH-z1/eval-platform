package com.mavis.evalplatform.evaluation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mavis.evalplatform.common.exception.BusinessException;
import com.mavis.evalplatform.common.exception.ErrorCode;
import com.mavis.evalplatform.common.result.PageResult;
import com.mavis.evalplatform.common.util.AesUtil;
import com.mavis.evalplatform.evaluation.adapter.ModelAdapter;
import com.mavis.evalplatform.evaluation.adapter.ModelAdapterFactory;
import com.mavis.evalplatform.evaluation.entity.Answer;
import com.mavis.evalplatform.evaluation.entity.Evaluation;
import com.mavis.evalplatform.evaluation.mapper.AnswerMapper;
import com.mavis.evalplatform.evaluation.mapper.EvaluationMapper;
import com.mavis.evalplatform.evaluation.service.EvaluationService;
import com.mavis.evalplatform.model.entity.ModelConfig;
import com.mavis.evalplatform.model.mapper.ModelConfigMapper;
import com.mavis.evalplatform.question.entity.Question;
import com.mavis.evalplatform.question.mapper.QuestionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 评测 Service 实现(FR-04)
 * <p>
 * 业务流程:
 * <ol>
 *   <li>create: 校验模型数 ≥ 1(实际跑需要 ≥ 2),问题数 ≤ 50,持久化 PENDING</li>
 *   <li>start: 状态 PENDING→RUNNING,启动异步任务,逐对 (问题, 模型) 调用 adapter</li>
 *   <li>全部完成:状态 RUNNING→COMPLETED(有失败标 FAILED)</li>
 *   <li>listAnswers: 按 evaluationId 查 answer 表</li>
 * </ol>
 * <p>
 * 简化实现(对齐任务范围,非完整架构):
 * <ul>
 *   <li>评测是同步执行(start 阻塞),后续可加 @Async 异步</li>
 *   <li>单题×单模型是并发执行(线程池 8)</li>
 *   <li>不实现 SSE 流式(后续可加)</li>
 *   <li>并发上限 = 模型数 × 问题数,默认不超过 50</li>
 * </ul>
 *
 * @author 梁倩倩
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationServiceImpl implements EvaluationService {

    private final EvaluationMapper evaluationMapper;
    private final AnswerMapper answerMapper;
    private final ModelConfigMapper modelConfigMapper;
    private final QuestionMapper questionMapper;
    private final ModelAdapterFactory adapterFactory;
    private final AesUtil aesUtil;

    private static final int MAX_QUESTIONS = 50;
    private static final int MAX_CONCURRENT = 8;

    private final ExecutorService executor = Executors.newFixedThreadPool(MAX_CONCURRENT, r -> {
        Thread t = new Thread(r, "eval-runner");
        t.setDaemon(true);
        return t;
    });

    // ============== 创建 ==============

    @Override
    @Transactional
    public Evaluation create(String name, String description, List<Long> modelIds, List<Long> questionIds, Long userId) {
        // 1) 入参校验
        if (modelIds == null || modelIds.isEmpty()) {
            throw new BusinessException(ErrorCode.EVALUATION_INVALID_MODELS, "请选择至少 1 个模型");
        }
        if (questionIds == null || questionIds.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "请选择至少 1 个问题");
        }
        if (questionIds.size() > MAX_QUESTIONS) {
            throw new BusinessException(ErrorCode.EVALUATION_TOO_MANY_QUESTIONS,
                    "单次评测问题数 " + questionIds.size() + " 超过上限 " + MAX_QUESTIONS);
        }
        if (modelIds.size() < 2) {
            // 仅警告,不阻断(单模型评测也允许)
            log.warn("[Eval] create with only {} model,建议至少 2 个做对比", modelIds.size());
        }

        // 2) 校验模型都存在且启用
        List<ModelConfig> models = modelConfigMapper.selectBatchIds(modelIds);
        if (models.size() != modelIds.size()) {
            throw new BusinessException(ErrorCode.MODEL_NOT_FOUND, "部分模型不存在");
        }
        long disabledCount = models.stream().filter(m -> m.getStatus() == null || m.getStatus() != 1).count();
        if (disabledCount > 0) {
            throw new BusinessException(ErrorCode.OPERATION_FAILED,
                    "有 " + disabledCount + " 个模型未启用,请先在模型配置页启用");
        }

        // 3) 持久化
        Evaluation e = new Evaluation();
        e.setName(name);
        e.setDescription(description);
        e.setCreatedBy(userId);
        e.setStatus("PENDING");
        e.setModelIds(joinIds(modelIds));
        e.setQuestionIds(joinIds(questionIds));
        e.setCreatedAt(LocalDateTime.now());
        evaluationMapper.insert(e);
        log.info("[Eval] created id={} name={} models={} questions={}", e.getId(), name, modelIds.size(), questionIds.size());
        return e;
    }

    // ============== 启动 ==============

    @Override
    @Transactional
    public void start(Long evaluationId) {
        Evaluation e = evaluationMapper.selectById(evaluationId);
        if (e == null) {
            throw new BusinessException(ErrorCode.EVALUATION_NOT_FOUND);
        }
        if ("RUNNING".equals(e.getStatus())) {
            throw new BusinessException(ErrorCode.EVALUATION_ALREADY_RUNNING);
        }
        if ("COMPLETED".equals(e.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_FAILED, "评测已完成,不可重复运行");
        }
        // 状态 PENDING/FAILED → RUNNING
        e.setStatus("RUNNING");
        e.setStartedAt(LocalDateTime.now());
        evaluationMapper.updateById(e);

        // 异步跑(用线程池,不阻塞 HTTP 请求)
        executor.submit(() -> runEvaluation(e));
    }

    /**
     * 真正执行评测 — 在线程池中跑
     */
    void runEvaluation(Evaluation e) {
        log.info("[Eval] run start id={}", e.getId());
        long start = System.currentTimeMillis();
        try {
            List<Long> modelIds = parseIds(e.getModelIds());
            List<Long> questionIds = parseIds(e.getQuestionIds());

            // 加载模型(解密 apiKey)
            List<ModelConfig> models = modelConfigMapper.selectBatchIds(modelIds);
            for (ModelConfig m : models) {
                try {
                    m.setApiKey(aesUtil.decrypt(m.getApiKey()));
                } catch (Exception ex) {
                    log.error("[Eval] decrypt apiKey failed for model {}", m.getId(), ex);
                }
            }

            // 加载问题
            List<Question> questions = questionMapper.selectBatchIds(questionIds);

            // 并发调用:对每对 (question, model) 提交任务
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            Semaphore sem = new Semaphore(MAX_CONCURRENT);
            for (Question q : questions) {
                for (ModelConfig m : models) {
                    final Question qq = q;
                    final ModelConfig mm = m;
                    futures.add(CompletableFuture.runAsync(() -> {
                        try {
                            sem.acquire();
                            runOne(e.getId(), mm, qq);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        } finally {
                            sem.release();
                        }
                    }, executor));
                }
            }
            // 等所有完成(带超时)
            try {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                        .get(10, TimeUnit.MINUTES);
            } catch (TimeoutException te) {
                log.error("[Eval] timeout", te);
            } catch (Exception ex) {
                log.error("[Eval] execute error", ex);
            }

            // 更新状态
            Evaluation done = evaluationMapper.selectById(e.getId());
            done.setStatus("COMPLETED");
            done.setFinishedAt(LocalDateTime.now());
            evaluationMapper.updateById(done);
            log.info("[Eval] run done id={} cost={}ms", e.getId(), System.currentTimeMillis() - start);
        } catch (Exception ex) {
            log.error("[Eval] run failed id={}", e.getId(), ex);
            Evaluation err = evaluationMapper.selectById(e.getId());
            if (err != null) {
                err.setStatus("FAILED");
                err.setFinishedAt(LocalDateTime.now());
                evaluationMapper.updateById(err);
            }
        }
    }

    /**
     * 单次调用 — 写一条 answer(成功或失败都写)
     */
    void runOne(Long evaluationId, ModelConfig model, Question question) {
        ModelAdapter adapter = adapterFactory.getAdapter(model.getProvider());
        ModelAdapter.ModelCallResult r = adapter.call(model, question.getContent());
        Answer a = new Answer();
        a.setEvaluationId(evaluationId);
        a.setQuestionId(question.getId());
        a.setModelId(model.getId());
        a.setLatencyMs((int) r.latencyMs());
        a.setTokenInput(r.tokenInput());
        a.setTokenOutput(r.tokenOutput());
        // 估算费用:输入 + 输出 总 token * 单价 / 1000
        if (model.getPricePerK() != null && r.tokenInput() != null && r.tokenOutput() != null) {
            BigDecimal cost = model.getPricePerK()
                    .multiply(BigDecimal.valueOf(r.tokenInput() + r.tokenOutput()))
                    .divide(BigDecimal.valueOf(1000), 4, RoundingMode.HALF_UP);
            a.setEstimatedCost(cost);
        }
        if (r.success()) {
            a.setContent(r.content());
        } else {
            a.setErrorCode(r.errorCode());
            a.setErrorMessage(r.errorMessage());
        }
        a.setCreatedAt(LocalDateTime.now());
        try {
            answerMapper.insert(a);
        } catch (Exception ex) {
            // UNIQUE KEY (evaluation_id, question_id, model_id) 冲突 — 已存在,跳过
            log.debug("[Eval] answer exists, skip: eval={} q={} m={}",
                    evaluationId, question.getId(), model.getId());
        }
    }

    // ============== 查询 ==============

    @Override
    public Evaluation getById(Long id) {
        Evaluation e = evaluationMapper.selectById(id);
        if (e == null) {
            throw new BusinessException(ErrorCode.EVALUATION_NOT_FOUND);
        }
        return e;
    }

    @Override
    public PageResult<Evaluation> page(long pageNum, long pageSize, Long creatorId, String status) {
        Page<Evaluation> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Evaluation> qw = new LambdaQueryWrapper<>();
        if (creatorId != null) {
            qw.eq(Evaluation::getCreatedBy, creatorId);
        }
        if (StringUtils.hasText(status)) {
            qw.eq(Evaluation::getStatus, status);
        }
        qw.orderByDesc(Evaluation::getCreatedAt);
        Page<Evaluation> result = evaluationMapper.selectPage(page, qw);
        return PageResult.of(result.getRecords(), result.getTotal(), pageNum, pageSize);
    }

    @Override
    public List<Answer> listAnswers(Long evaluationId) {
        LambdaQueryWrapper<Answer> qw = new LambdaQueryWrapper<>();
        qw.eq(Answer::getEvaluationId, evaluationId);
        qw.orderByAsc(Answer::getQuestionId, Answer::getModelId);
        return answerMapper.selectList(qw);
    }

    // ============== 工具 ==============

    private static String joinIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return "";
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private static List<Long> parseIds(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        try {
            return Arrays.stream(csv.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .toList();
        } catch (Exception ex) {
            return List.of();
        }
    }
}
