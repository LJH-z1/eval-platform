package com.mavis.evalplatform.stats.service.impl;

import com.mavis.evalplatform.common.exception.BusinessException;
import com.mavis.evalplatform.common.exception.ErrorCode;
import com.mavis.evalplatform.evaluation.entity.Answer;
import com.mavis.evalplatform.evaluation.entity.Evaluation;
import com.mavis.evalplatform.evaluation.mapper.AnswerMapper;
import com.mavis.evalplatform.evaluation.mapper.EvaluationMapper;
import com.mavis.evalplatform.model.entity.ModelConfig;
import com.mavis.evalplatform.model.mapper.ModelConfigMapper;
import com.mavis.evalplatform.score.entity.Score;
import com.mavis.evalplatform.score.mapper.ScoreMapper;
import com.mavis.evalplatform.stats.service.FleissKappaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Fleiss Kappa + 一致性分析(FR-06)
 *
 * @author 宋子翔
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FleissKappaServiceImpl implements FleissKappaService {

    private final ScoreMapper scoreMapper;
    private final AnswerMapper answerMapper;
    private final EvaluationMapper evaluationMapper;
    private final ModelConfigMapper modelConfigMapper;

    /** 一致性阈值:同问题标准差 > 1.5 算争议 */
    private static final double CONTROVERSY_STD_THRESHOLD = 1.5;
    /** 模型加权权重(对齐 §3.6.4) */
    private static final double W_ACCURACY = 0.4;
    private static final double W_RELEVANCE = 0.3;
    private static final double W_FLUENCY   = 0.2;
    private static final double W_SAFETY    = 0.1;

    @Override
    public double calculate(List<List<Integer>> scores, int kCategories) {
        if (scores == null || scores.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "Fleiss Kappa 输入为空");
        }
        int N = scores.size();
        int n = scores.get(0).size();
        if (n < 2) {
            return 1.0; // 只有 1 个评分员,默认完美一致
        }
        int k = kCategories;

        // 1) 建表 count[i][j]: 第 i 个对象在 j 类别的评分员数
        int[][] count = new int[N][k];
        for (int i = 0; i < N; i++) {
            if (scores.get(i).size() != n) {
                throw new BusinessException(ErrorCode.PARAM_INVALID,
                        "Fleiss Kappa:第 " + i + " 行的评分员数(" + scores.get(i).size() + ")与第一行(" + n + ")不一致");
            }
            for (int j = 0; j < n; j++) {
                int cat = scores.get(i).get(j) - 1; // 1-5 -> 0-4
                if (cat < 0 || cat >= k) {
                    throw new BusinessException(ErrorCode.PARAM_INVALID,
                            "Fleiss Kappa:第 " + i + " 行第 " + j + " 列的评分 " + scores.get(i).get(j) + " 超出 [1, " + k + "]");
                }
                count[i][cat]++;
            }
        }

        // 2) 计算每个对象的一致性 Pi
        double sumP = 0;
        for (int i = 0; i < N; i++) {
            double pi = 0;
            for (int j = 0; j < k; j++) pi += Math.pow(count[i][j], 2);
            pi = (pi - n) / (double) (n * (n - 1));
            sumP += pi;
        }
        double P = sumP / N;

        // 3) 计算每个类别的比例 pj
        double[] pj = new double[k];
        for (int j = 0; j < k; j++) {
            for (int i = 0; i < N; i++) pj[j] += count[i][j];
            pj[j] /= (double) (N * n);
        }

        // 4) 期望一致 Pe
        double Pe = 0;
        for (int j = 0; j < k; j++) Pe += Math.pow(pj[j], 2);

        // 5) Kappa
        if (Math.abs(1 - Pe) < 1e-9) {
            return 1.0; // 全部都是同一类别
        }
        return (P - Pe) / (1 - Pe);
    }

    /**
     * 收集某评测某维度的所有评分,转换为 N×n 矩阵
     * @return matrix / 模型信息列表 / 评分员数
     */
    private KappaData collectKappaData(Long evaluationId, String dimension) {
        Evaluation e = evaluationMapper.selectById(evaluationId);
        if (e == null) throw new BusinessException(ErrorCode.EVALUATION_NOT_FOUND);

        // 查所有 answer
        List<Answer> answers = answerMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Answer>()
                        .eq(Answer::getEvaluationId, evaluationId));
        if (answers.isEmpty()) {
            return new KappaData(e, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }
        Set<Long> answerIds = answers.stream().map(Answer::getId).collect(Collectors.toSet());

        // 查所有 score(限定这些 answer)
        List<Score> scores = scoreMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Score>()
                        .in(Score::getAnswerId, answerIds));

        // 按 answerId 分组
        Map<Long, List<Score>> byAnswer = new HashMap<>();
        for (Score s : scores) {
            byAnswer.computeIfAbsent(s.getAnswerId(), k -> new ArrayList<>()).add(s);
        }

        // 对每个 answer,收集该维度的所有评分员打分
        List<List<Integer>> matrix = new ArrayList<>();
        for (Answer a : answers) {
            List<Score> sl = byAnswer.getOrDefault(a.getId(), Collections.emptyList());
            if (sl.isEmpty()) continue;
            // 同一评分员对同一回答只能有 1 个 score(UNIQUE 约束)
            List<Integer> row = new ArrayList<>();
            for (Score s : sl) {
                int val = extractDimension(s, dimension);
                row.add(val);
            }
            if (!row.isEmpty()) matrix.add(row);
        }

        return new KappaData(e, answers, matrix, scores);
    }

    private int extractDimension(Score s, String dim) {
        return switch (dim.toLowerCase()) {
            case "accuracy"  -> s.getAccuracy()  == null ? 0 : s.getAccuracy();
            case "relevance" -> s.getRelevance() == null ? 0 : s.getRelevance();
            case "fluency"   -> s.getFluency()   == null ? 0 : s.getFluency();
            case "safety"    -> s.getSafety()    == null ? 0 : s.getSafety();
            default -> s.getAccuracy() == null ? 0 : s.getAccuracy();
        };
    }

    @Override
    public Map<String, Object> getKappa(Long evaluationId) {
        KappaData data = collectKappaData(evaluationId, "accuracy");

        // 统计信息
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("evaluationId", evaluationId);
        result.put("evaluationName", data.eval.getName());
        result.put("totalAnswers", data.answers.size());
        result.put("totalScores", data.scores.size());
        result.put("scorerCount", data.scores.stream().map(Score::getScorerId).distinct().count());

        // 4 维度各算一次 Kappa
        Map<String, Double> kappas = new LinkedHashMap<>();
        String[] dims = {"accuracy", "relevance", "fluency", "safety"};
        for (String dim : dims) {
            KappaData d = collectKappaData(evaluationId, dim);
            if (d.matrix.isEmpty() || d.matrix.get(0).size() < 2) {
                kappas.put(dim, null); // 数据不足
                continue;
            }
            try {
                kappas.put(dim, round(calculate(d.matrix, 5), 4));
            } catch (Exception ex) {
                kappas.put(dim, null);
            }
        }
        result.put("kappas", kappas);
        result.put("interpretation", interpretKappa(kappas.get("accuracy")));

        // 警告
        List<String> warnings = new ArrayList<>();
        if (kappas.get("accuracy") == null) {
            warnings.add("评分员 < 2 或没有评分,Kappa 不可计算");
        } else if (kappas.get("accuracy") < 0) {
            warnings.add("Kappa < 0,评分员一致性极差");
        }
        result.put("warnings", warnings);

        return result;
    }

    @Override
    public List<Map<String, Object>> getControversialItems(Long evaluationId) {
        List<Answer> answers = answerMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Answer>()
                        .eq(Answer::getEvaluationId, evaluationId));
        if (answers.isEmpty()) return Collections.emptyList();

        Set<Long> answerIds = answers.stream().map(Answer::getId).collect(Collectors.toSet());
        List<Score> scores = scoreMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Score>()
                        .in(Score::getAnswerId, answerIds));

        Map<Long, List<Score>> byAnswer = new HashMap<>();
        for (Score s : scores) byAnswer.computeIfAbsent(s.getAnswerId(), k -> new ArrayList<>()).add(s);

        // 加载问题文本
        Map<Long, String> qText = new HashMap<>();
        // 简化为直接用 questionId 作 key
        List<Map<String, Object>> result = new ArrayList<>();
        for (Answer a : answers) {
            List<Score> sl = byAnswer.getOrDefault(a.getId(), Collections.emptyList());
            if (sl.size() < 2) continue;
            // 用 4 维度的平均值算 std
            List<Double> avgs = new ArrayList<>();
            for (Score s : sl) {
                double avg = (s.getAccuracy() + s.getRelevance() + s.getFluency() + s.getSafety()) / 4.0;
                avgs.add(avg);
            }
            double mean = avgs.stream().mapToDouble(d -> d).average().orElse(0);
            double variance = avgs.stream().mapToDouble(d -> Math.pow(d - mean, 2)).average().orElse(0);
            double std = Math.sqrt(variance);
            if (std > CONTROVERSY_STD_THRESHOLD) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("answerId", a.getId());
                row.put("questionId", a.getQuestionId());
                row.put("modelId", a.getModelId());
                row.put("std", round(std, 3));
                row.put("meanScore", round(mean, 2));
                row.put("scorerCount", sl.size());
                row.put("scorerAvgs", avgs.stream().map(d -> round(d, 2)).toList());
                result.add(row);
            }
        }
        // 按争议度降序
        result.sort((x, y) -> Double.compare((double) y.get("std"), (double) x.get("std")));
        return result;
    }

    @Override
    public List<Map<String, Object>> getScorerRanking(Long evaluationId) {
        List<Score> scores = scoreMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Score>()
                        .apply("answer_id IN (SELECT id FROM answer WHERE evaluation_id = {0})", evaluationId));
        Map<Long, List<Score>> byScorer = new HashMap<>();
        for (Score s : scores) byScorer.computeIfAbsent(s.getScorerId(), k -> new ArrayList<>()).add(s);

        // 查所有 answer
        List<Answer> allAnswers = answerMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Answer>()
                        .eq(Answer::getEvaluationId, evaluationId));
        long totalAnswers = allAnswers.size();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, List<Score>> e : byScorer.entrySet()) {
            List<Score> sl = e.getValue();
            double avg = sl.stream().mapToInt(s -> (s.getAccuracy() + s.getRelevance() + s.getFluency() + s.getSafety()) / 4).average().orElse(0);
            double coverage = totalAnswers == 0 ? 0 : (sl.size() * 100.0 / totalAnswers);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("scorerId", e.getKey());
            row.put("scoredCount", sl.size());
            row.put("avgScore", round(avg, 2));
            row.put("coveragePct", round(coverage, 1));
            result.add(row);
        }
        result.sort((x, y) -> Integer.compare((int) y.get("scoredCount"), (int) x.get("scoredCount")));
        return result;
    }

    @Override
    public List<Map<String, Object>> getModelRanking(Long evaluationId) {
        // 按 modelId 聚合
        List<Answer> answers = answerMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Answer>()
                        .eq(Answer::getEvaluationId, evaluationId));
        if (answers.isEmpty()) return Collections.emptyList();
        Set<Long> answerIds = answers.stream().map(Answer::getId).collect(Collectors.toSet());
        List<Score> scores = scoreMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Score>()
                        .in(Score::getAnswerId, answerIds));

        Map<Long, List<Score>> byModel = new HashMap<>();
        Map<Long, Answer> ansById = new HashMap<>();
        for (Answer a : answers) {
            ansById.put(a.getId(), a);
            byModel.computeIfAbsent(a.getModelId(), k -> new ArrayList<>());
        }
        for (Score s : scores) {
            Answer a = ansById.get(s.getAnswerId());
            if (a != null) byModel.computeIfAbsent(a.getModelId(), k -> new ArrayList<>()).add(s);
        }

        // 加载模型名
        Set<Long> modelIds = byModel.keySet();
        List<ModelConfig> models = modelConfigMapper.selectBatchIds(modelIds);
        Map<Long, ModelConfig> modelMap = new HashMap<>();
        for (ModelConfig m : models) modelMap.put(m.getId(), m);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, List<Score>> e : byModel.entrySet()) {
            List<Score> sl = e.getValue();
            if (sl.isEmpty()) continue;
            double acc = sl.stream().mapToInt(Score::getAccuracy).average().orElse(0);
            double rel = sl.stream().mapToInt(Score::getRelevance).average().orElse(0);
            double flu = sl.stream().mapToInt(Score::getFluency).average().orElse(0);
            double saf = sl.stream().mapToInt(Score::getSafety).average().orElse(0);
            double weighted = acc * W_ACCURACY + rel * W_RELEVANCE + flu * W_FLUENCY + saf * W_SAFETY;

            ModelConfig m = modelMap.get(e.getKey());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("modelId", e.getKey());
            row.put("modelName", m == null ? "Model#" + e.getKey() : m.getName());
            row.put("provider", m == null ? "?" : m.getProvider());
            row.put("accuracy", round(acc, 2));
            row.put("relevance", round(rel, 2));
            row.put("fluency", round(flu, 2));
            row.put("safety", round(saf, 2));
            row.put("weightedScore", round(weighted, 3));
            row.put("scoredCount", sl.size());
            result.add(row);
        }
        result.sort((x, y) -> Double.compare((double) y.get("weightedScore"), (double) x.get("weightedScore")));
        return result;
    }

    // ====== 工具方法 ======

    private String interpretKappa(Double k) {
        if (k == null) return "数据不足";
        if (k < 0)    return "差(比随机还差)";
        if (k < 0.2)  return "轻微一致";
        if (k < 0.4)  return "一般一致";
        if (k < 0.6)  return "中等一致";
        if (k < 0.8)  return "良好一致";
        return "优秀一致";
    }

    private double round(double v, int scale) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return 0;
        return BigDecimal.valueOf(v).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }

    private record KappaData(Evaluation eval, List<Answer> answers,
                             List<List<Integer>> matrix, List<Score> scores) {}
}
