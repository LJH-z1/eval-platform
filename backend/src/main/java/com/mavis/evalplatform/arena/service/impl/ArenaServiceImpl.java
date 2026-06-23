package com.mavis.evalplatform.arena.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mavis.evalplatform.arena.entity.ArenaVote;
import com.mavis.evalplatform.arena.mapper.ArenaVoteMapper;
import com.mavis.evalplatform.arena.service.ArenaService;
import com.mavis.evalplatform.common.exception.BusinessException;
import com.mavis.evalplatform.common.exception.ErrorCode;
import com.mavis.evalplatform.common.util.AesUtil;
import com.mavis.evalplatform.evaluation.adapter.ModelAdapter;
import com.mavis.evalplatform.evaluation.adapter.ModelAdapterFactory;
import com.mavis.evalplatform.evaluation.entity.Answer;
import com.mavis.evalplatform.evaluation.entity.Evaluation;
import com.mavis.evalplatform.evaluation.mapper.AnswerMapper;
import com.mavis.evalplatform.evaluation.mapper.EvaluationMapper;
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

/**
 * Arena 盲评 + Elo 排名实现
 * <p>
 * quickEvaluate 同步跑 1 题 2 模型:复用 {@link ModelAdapterFactory} + 持久化 evaluation/answer(让评测任务页也能看到)
 * <p>
 * Elo:K=32,初始 1500;按投票时间顺序累加更新,全量重算
 *
 * @author 刘家豪
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArenaServiceImpl implements ArenaService {

    private final ModelAdapterFactory adapterFactory;
    private final ModelConfigMapper modelConfigMapper;
    private final QuestionMapper questionMapper;
    private final EvaluationMapper evaluationMapper;
    private final AnswerMapper answerMapper;
    private final ArenaVoteMapper arenaVoteMapper;
    private final AesUtil aesUtil;

    /** 单题并行上限 */
    private static final int QUICK_CONCURRENT = 4;
    private final ExecutorService quickExec = Executors.newFixedThreadPool(QUICK_CONCURRENT, r -> {
        Thread t = new Thread(r, "arena-quick");
        t.setDaemon(true);
        return t;
    });

    @Override
    public Map<String, Object> quickEvaluate(String prompt, Long modelAId, Long modelBId, Long userId) {
        validate(prompt, modelAId, modelBId);
        return doOne(prompt, modelAId, modelBId, userId);
    }

    @Override
    public List<Map<String, Object>> batchEvaluate(List<String> prompts, Long modelAId, Long modelBId, Long userId) {
        if (prompts == null || prompts.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "prompts 不能为空");
        }
        if (prompts.size() > 30) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "批量最多 30 题(当前 " + prompts.size() + ")");
        }
        if (modelAId == null || modelBId == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "请选择 2 个模型");
        }
        if (modelAId.equals(modelBId)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "两个模型不能相同");
        }
        // 校验每个 prompt
        for (int i = 0; i < prompts.size(); i++) {
            String p = prompts.get(i);
            if (!StringUtils.hasText(p)) throw new BusinessException(ErrorCode.PARAM_INVALID, "第 " + (i+1) + " 题不能为空");
            if (p.length() > 4000) throw new BusinessException(ErrorCode.PARAM_INVALID, "第 " + (i+1) + " 题长度超过 4000");
        }
        log.info("[Arena] batch-eval start: {} 题 × 2 模型 ({} vs {})", prompts.size(), modelAId, modelBId);
        // 预校验模型(只查 1 次)
        List<ModelConfig> models = modelConfigMapper.selectBatchIds(List.of(modelAId, modelBId));
        if (models.size() != 2) throw new BusinessException(ErrorCode.MODEL_NOT_FOUND, "模型不存在");

        // 并发跑每题
        List<CompletableFuture<Map<String, Object>>> futures = new ArrayList<>();
        for (int i = 0; i < prompts.size(); i++) {
            final String p = prompts.get(i);
            final int idx = i;
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    Map<String, Object> r = doOne(p, modelAId, modelBId, userId);
                    r.put("batchIndex", idx);
                    return r;
                } catch (Exception ex) {
                    log.error("[Arena] batch item {} failed", idx, ex);
                    Map<String, Object> err = new HashMap<>();
                    err.put("batchIndex", idx);
                    err.put("prompt", p);
                    err.put("error", ex.getMessage());
                    return err;
                }
            }, quickExec));
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (CompletableFuture<Map<String, Object>> f : futures) out.add(f.join());
        // 按 batchIndex 排序
        out.sort((a, b) -> Integer.compare((int) a.get("batchIndex"), (int) b.get("batchIndex")));
        log.info("[Arena] batch-eval done: {} 题", out.size());
        return out;
    }

    private static void validate(String prompt, Long modelAId, Long modelBId) {
        if (!StringUtils.hasText(prompt)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "prompt 不能为空");
        }
        if (prompt.length() > 4000) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "prompt 长度超过 4000");
        }
        if (modelAId == null || modelBId == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "请选择 2 个模型");
        }
        if (modelAId.equals(modelBId)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "两个模型不能相同");
        }
    }

    /**
     * 单题执行(quickEvaluate 和 batchEvaluate 共用)— 自己开新事务
     * 因为 batchEvaluate 在多线程下复用 quickEvaluate 的 @Transactional 会冲突
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public Map<String, Object> doOne(String prompt, Long modelAId, Long modelBId, Long userId) {
        // 1) 加载模型
        List<ModelConfig> models = modelConfigMapper.selectBatchIds(List.of(modelAId, modelBId));
        if (models.size() != 2) {
            throw new BusinessException(ErrorCode.MODEL_NOT_FOUND, "模型不存在");
        }
        ModelConfig mA = models.stream().filter(m -> m.getId().equals(modelAId)).findFirst().orElseThrow();
        ModelConfig mB = models.stream().filter(m -> m.getId().equals(modelBId)).findFirst().orElseThrow();
        // 解密 apiKey
        decryptInPlace(mA);
        decryptInPlace(mB);

        // 2) 创建 question(入库,isPublic=false 让题库页不可见)
        Question q = new Question();
        q.setContent(prompt);
        q.setType("ARENA");
        q.setCategory("Arena");
        q.setDifficulty("MEDIUM");
        q.setIsPublic(0);
        q.setCreatedBy(userId);
        q.setCreatedAt(LocalDateTime.now());
        q.setUpdatedAt(LocalDateTime.now());
        questionMapper.insert(q);

        // 3) 同步并发调 2 个 adapter
        ModelAdapter.ModelCallResult rA = callAsync(mA, prompt);
        ModelAdapter.ModelCallResult rB = callAsync(mB, prompt);

        // 4) 创建 evaluation(关联 question + 2 模型)
        Evaluation eval = new Evaluation();
        eval.setName("Arena-" + System.currentTimeMillis());
        eval.setDescription("Arena 盲评");
        eval.setCreatedBy(userId);
        eval.setStatus("COMPLETED");
        eval.setModelIds(modelAId + "," + modelBId);
        eval.setQuestionIds(String.valueOf(q.getId()));
        eval.setCreatedAt(LocalDateTime.now());
        eval.setStartedAt(LocalDateTime.now());
        eval.setFinishedAt(LocalDateTime.now());
        evaluationMapper.insert(eval);

        // 5) 写 answer(2 条)
        Answer ansA = buildAnswer(eval.getId(), q.getId(), mA, rA);
        Answer ansB = buildAnswer(eval.getId(), q.getId(), mB, rB);
        answerMapper.insert(ansA);
        answerMapper.insert(ansB);

        // 6) 返回
        Map<String, Object> left = new HashMap<>();
        left.put("answerId", ansA.getId());
        left.put("content", rA.content());
        left.put("latencyMs", rA.latencyMs());
        left.put("modelId", mA.getId());
        left.put("modelName", mA.getName());
        left.put("modelProvider", mA.getProvider());
        left.put("success", rA.success());
        if (!rA.success()) {
            left.put("errorCode", rA.errorCode());
            left.put("errorMessage", rA.errorMessage());
        }

        Map<String, Object> right = new HashMap<>();
        right.put("answerId", ansB.getId());
        right.put("content", rB.content());
        right.put("latencyMs", rB.latencyMs());
        right.put("modelId", mB.getId());
        right.put("modelName", mB.getName());
        right.put("modelProvider", mB.getProvider());
        right.put("success", rB.success());
        if (!rB.success()) {
            right.put("errorCode", rB.errorCode());
            right.put("errorMessage", rB.errorMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("evaluationId", eval.getId());
        result.put("prompt", prompt);
        result.put("left", left);
        result.put("right", right);
        return result;
    }

    /** 同步并发跑 2 个模型 — 失败时 fallback 到 MOCK,让 Arena 永远能拿到 2 个回答 */
    private ModelAdapter.ModelCallResult callAsync(ModelConfig m, String prompt) {
        long start = System.currentTimeMillis();
        ModelAdapter.ModelCallResult real;
        try {
            CompletableFuture<ModelAdapter.ModelCallResult> fA = CompletableFuture.supplyAsync(
                () -> adapterFactory.getAdapter(m.getProvider()).call(m, prompt), quickExec);
            real = fA.get(2, TimeUnit.MINUTES);
        } catch (TimeoutException te) {
            real = ModelAdapter.ModelCallResult.fail("TIMEOUT", "模型调用超时(2min)", System.currentTimeMillis() - start);
        } catch (Exception ex) {
            log.error("[Arena] call model={} failed", m.getId(), ex);
            real = ModelAdapter.ModelCallResult.fail("INTERNAL_ERROR", ex.getMessage(), System.currentTimeMillis() - start);
        }

        // === Fallback:真模型失败时返回 MOCK 内容,这样 Arena 永远有 2 个回答可对比 ===
        if (real.success()) return real;

        String origError = real.errorMessage() == null ? "未知错误" : truncate(real.errorMessage(), 200);
        log.warn("[Arena] model={} failed, fallback to MOCK: {}", m.getId(), origError);
        String mockContent = "[FALLBACK-MOCK · " + m.getProvider() + " · " + m.getName() + "]\n"
                + "原模型调用失败: " + origError + "\n"
                + "────────────────────────────────\n"
                + "以下为模拟回答,真实对比请修复原模型(" + m.getProvider() + "):\n\n"
                + buildMockAnswer(prompt, m.getName());
        return ModelAdapter.ModelCallResult.ok(mockContent, 0, 0, System.currentTimeMillis() - start);
    }

    private static String buildMockAnswer(String prompt, String modelName) {
        // 按 provider 给点差异化(用 MOCK 前缀区分),但答案大致相似让用户能投 tie
        String prefix = "【模拟-" + (modelName == null ? "MODEL" : modelName) + "】";
        String head = prefix + " 关于 \"" + (prompt.length() > 30 ? prompt.substring(0, 30) + "..." : prompt) + "\" 的回答:\n\n";
        String body = "这是一个经过 fallback 的模拟回答,用于在真实模型失败时仍能让 Arena 完成对比。\n\n"
                + "常见要点:\n"
                + "1. 直接切题,先给结论\n"
                + "2. 列举 2-3 个关键要素\n"
                + "3. 给出可执行的建议\n\n"
                + "(若想看到真实对比,请检查模型 " + (modelName == null ? "" : modelName) + " 的 endpoint / apiKey / 余额 / 网络访问)。";
        return head + body;
    }

    private static String truncate(String s, int n) {
        if (s == null) return "";
        return s.length() > n ? s.substring(0, n) + "..." : s;
    }

    private void decryptInPlace(ModelConfig m) {
        try {
            if (StringUtils.hasText(m.getApiKey())) {
                m.setApiKey(aesUtil.decrypt(m.getApiKey()));
            }
        } catch (Exception ex) {
            log.warn("[Arena] decrypt apiKey for model {} failed: {}", m.getId(), ex.getMessage());
        }
    }

    private Answer buildAnswer(Long evalId, Long qId, ModelConfig m, ModelAdapter.ModelCallResult r) {
        Answer a = new Answer();
        a.setEvaluationId(evalId);
        a.setQuestionId(qId);
        a.setModelId(m.getId());
        a.setContent(r.success() ? r.content() : (r.content() != null ? r.content() : "(无回答)"));
        a.setLatencyMs((int) Math.min(Integer.MAX_VALUE, r.latencyMs()));
        a.setTokenInput(r.tokenInput() != null ? r.tokenInput() : 0);
        a.setTokenOutput(r.tokenOutput() != null ? r.tokenOutput() : 0);
        a.setEstimatedCost(BigDecimal.ZERO);
        a.setErrorCode(r.success() ? null : r.errorCode());
        a.setErrorMessage(r.success() ? null : r.errorMessage());
        a.setCreatedAt(LocalDateTime.now());
        return a;
    }

    // ============== 投票 + Elo ==============

    @Override
    @Transactional
    public Long vote(Long evaluationId, String prompt, Long leftModelId, Long rightModelId, String winner, Long userId) {
        if (!List.of("A", "B", "tie", "bad").contains(winner)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "winner 必须是 A/B/tie/bad");
        }
        ArenaVote v = new ArenaVote();
        v.setEvaluationId(evaluationId);
        v.setVoterId(userId);
        v.setPrompt(prompt);
        v.setLeftModelId(leftModelId);
        v.setRightModelId(rightModelId);
        v.setWinner(winner);
        v.setCreatedAt(LocalDateTime.now());
        arenaVoteMapper.insert(v);
        log.info("[Arena] vote id={} voter={} left={} right={} winner={}",
                v.getId(), userId, leftModelId, rightModelId, winner);
        return v.getId();
    }

    @Override
    public List<Map<String, Object>> ranking() {
        // 全量投票
        List<ArenaVote> votes = arenaVoteMapper.selectList(
            new LambdaQueryWrapper<ArenaVote>().orderByAsc(ArenaVote::getCreatedAt));

        // 模型 Elo 表(只在参与过投票的模型里建)
        Map<Long, Integer> elo = new HashMap<>();
        Map<Long, Integer> games = new HashMap<>();
        Map<Long, Integer> wins = new HashMap<>();
        Map<Long, Integer> losses = new HashMap<>();
        Map<Long, Integer> ties = new HashMap<>();

        // 默认 Elo 1500
        for (ArenaVote v : votes) {
            elo.putIfAbsent(v.getLeftModelId(), 1500);
            elo.putIfAbsent(v.getRightModelId(), 1500);
        }

        // 累加更新
        final int K = 32;
        for (ArenaVote v : votes) {
            long A = v.getLeftModelId(), B = v.getRightModelId();
            int rA = elo.get(A), rB = elo.get(B);
            double eA = 1.0 / (1.0 + Math.pow(10, (rB - rA) / 400.0));
            double eB = 1.0 - eA;
            double sA, sB;
            switch (v.getWinner()) {
                case "A"   -> { sA = 1.0; sB = 0.0; wins.merge(A, 1, Integer::sum); losses.merge(B, 1, Integer::sum); }
                case "B"   -> { sA = 0.0; sB = 1.0; wins.merge(B, 1, Integer::sum); losses.merge(A, 1, Integer::sum); }
                case "tie" -> { sA = 0.5; sB = 0.5; ties.merge(A, 1, Integer::sum); ties.merge(B, 1, Integer::sum); }
                case "bad" -> { sA = 0.0; sB = 0.0; /* 都记 loss */ losses.merge(A, 1, Integer::sum); losses.merge(B, 1, Integer::sum); }
                default    -> { sA = 0.5; sB = 0.5; }
            }
            elo.put(A, (int) Math.round(rA + K * (sA - eA)));
            elo.put(B, (int) Math.round(rB + K * (sB - eB)));
            games.merge(A, 1, Integer::sum);
            games.merge(B, 1, Integer::sum);
        }

        // 加载模型名
        Map<Long, ModelConfig> modelMap = new HashMap<>();
        if (!elo.isEmpty()) {
            modelMap.putAll(modelConfigMapper.selectBatchIds(elo.keySet())
                .stream().collect(java.util.stream.Collectors.toMap(ModelConfig::getId, m -> m)));
        }

        // 按 Elo 降序
        List<Map<String, Object>> list = new ArrayList<>();
        elo.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .forEach(e -> {
                Long mid = e.getKey();
                ModelConfig m = modelMap.get(mid);
                if (m == null) return;
                int total = games.getOrDefault(mid, 0);
                int winN  = wins.getOrDefault(mid, 0);
                int lossN = losses.getOrDefault(mid, 0);
                int tieN  = ties.getOrDefault(mid, 0);
                Map<String, Object> row = new HashMap<>();
                row.put("rank", 0); // 稍后填
                row.put("modelId", mid);
                row.put("modelName", m.getName());
                row.put("provider", m.getProvider());
                row.put("elo", e.getValue());
                row.put("games", total);
                row.put("wins", winN);
                row.put("losses", lossN);
                row.put("ties", tieN);
                row.put("winRate", total == 0 ? 0.0 : BigDecimal.valueOf((double) winN / total).setScale(3, RoundingMode.HALF_UP).doubleValue());
                list.add(row);
            });
        // 填 rank
        for (int i = 0; i < list.size(); i++) list.get(i).put("rank", i + 1);
        return list;
    }
}
