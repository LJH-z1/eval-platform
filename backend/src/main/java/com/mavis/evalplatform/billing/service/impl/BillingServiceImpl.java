package com.mavis.evalplatform.billing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mavis.evalplatform.billing.service.BillingService;
import com.mavis.evalplatform.common.exception.BusinessException;
import com.mavis.evalplatform.common.exception.ErrorCode;
import com.mavis.evalplatform.evaluation.entity.Answer;
import com.mavis.evalplatform.evaluation.entity.Evaluation;
import com.mavis.evalplatform.evaluation.mapper.AnswerMapper;
import com.mavis.evalplatform.evaluation.mapper.EvaluationMapper;
import com.mavis.evalplatform.model.entity.ModelConfig;
import com.mavis.evalplatform.model.mapper.ModelConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 成本统计实现(FR-07)
 *
 * @author 梁倩倩
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BillingServiceImpl implements BillingService {

    private final AnswerMapper answerMapper;
    private final EvaluationMapper evaluationMapper;
    private final ModelConfigMapper modelConfigMapper;

    @Override
    public Map<String, Object> summary(Long evaluationId) {
        Evaluation e = evaluationMapper.selectById(evaluationId);
        if (e == null) throw new BusinessException(ErrorCode.EVALUATION_NOT_FOUND);

        List<Answer> answers = answerMapper.selectList(
                new LambdaQueryWrapper<Answer>().eq(Answer::getEvaluationId, evaluationId));
        if (answers.isEmpty()) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("evaluationId", evaluationId);
            r.put("evaluationName", e.getName());
            r.put("totalCalls", 0);
            r.put("successCalls", 0);
            r.put("failCalls", 0);
            r.put("totalInputTokens", 0L);
            r.put("totalOutputTokens", 0L);
            r.put("totalCost", BigDecimal.ZERO);
            r.put("avgLatencyMs", 0);
            return r;
        }

        long totalIn = answers.stream().filter(a -> a.getTokenInput() != null).mapToLong(Answer::getTokenInput).sum();
        long totalOut = answers.stream().filter(a -> a.getTokenOutput() != null).mapToLong(Answer::getTokenOutput).sum();
        BigDecimal totalCost = answers.stream()
                .filter(a -> a.getEstimatedCost() != null)
                .map(Answer::getEstimatedCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);
        // error_code 为 null = 成功,非 null = 失败
        long success = answers.stream().filter(a -> a.getErrorCode() == null).count();
        long fail = answers.size() - success;
        double avgLat = answers.stream()
                .filter(a -> a.getLatencyMs() != null)
                .mapToInt(Answer::getLatencyMs)
                .average().orElse(0);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("evaluationId", evaluationId);
        r.put("evaluationName", e.getName());
        r.put("totalCalls", answers.size());
        r.put("successCalls", success);
        r.put("failCalls", fail);
        r.put("totalInputTokens", totalIn);
        r.put("totalOutputTokens", totalOut);
        r.put("totalTokens", totalIn + totalOut);
        r.put("totalCost", totalCost);
        r.put("avgLatencyMs", round(avgLat, 0));
        return r;
    }

    @Override
    public Map<String, Object> timeSeries(Long evaluationId, String granularity) {
        if (granularity == null) granularity = "hour";
        List<Answer> answers = answerMapper.selectList(
                new LambdaQueryWrapper<Answer>().eq(Answer::getEvaluationId, evaluationId));

        Map<String, Bucket> buckets = new TreeMap<>();
        DateTimeFormatter fmt = "hour".equals(granularity) ? DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00") : DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (Answer a : answers) {
            if (a.getCreatedAt() == null) continue;
            String key = a.getCreatedAt().format(fmt);
            Bucket b = buckets.computeIfAbsent(key, k -> new Bucket());
            b.calls++;
            if (a.getEstimatedCost() != null) b.cost = b.cost.add(a.getEstimatedCost());
        }

        List<String> xAxis = new ArrayList<>(buckets.keySet());
        List<Long> callSeries = new ArrayList<>();
        List<Double> costSeries = new ArrayList<>();
        for (String k : xAxis) {
            Bucket b = buckets.get(k);
            callSeries.add(b.calls);
            costSeries.add(round(b.cost.doubleValue(), 4));
        }

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("xAxis", xAxis);
        r.put("series", List.of(
                Map.of("name", "调用量", "type", "line", "data", callSeries),
                Map.of("name", "费用(元)", "type", "line", "data", costSeries)
        ));
        return r;
    }

    @Override
    public List<Map<String, Object>> byModel(Long evaluationId) {
        List<Answer> answers = answerMapper.selectList(
                new LambdaQueryWrapper<Answer>().eq(Answer::getEvaluationId, evaluationId));
        if (answers.isEmpty()) return Collections.emptyList();

        Set<Long> modelIds = answers.stream().map(Answer::getModelId).collect(Collectors.toSet());
        List<ModelConfig> models = modelConfigMapper.selectBatchIds(modelIds);
        Map<Long, ModelConfig> modelMap = new HashMap<>();
        for (ModelConfig m : models) modelMap.put(m.getId(), m);

        Map<Long, List<Answer>> byModel = new HashMap<>();
        for (Answer a : answers) byModel.computeIfAbsent(a.getModelId(), k -> new ArrayList<>()).add(a);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, List<Answer>> e : byModel.entrySet()) {
            List<Answer> list = e.getValue();
            long inTok = list.stream().filter(a -> a.getTokenInput() != null).mapToLong(Answer::getTokenInput).sum();
            long outTok = list.stream().filter(a -> a.getTokenOutput() != null).mapToLong(Answer::getTokenOutput).sum();
            BigDecimal cost = list.stream()
                    .filter(a -> a.getEstimatedCost() != null)
                    .map(Answer::getEstimatedCost)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(4, RoundingMode.HALF_UP);
            double avgLat = list.stream().filter(a -> a.getLatencyMs() != null)
                    .mapToInt(Answer::getLatencyMs).average().orElse(0);
            ModelConfig m = modelMap.get(e.getKey());

            Map<String, Object> r = new LinkedHashMap<>();
            r.put("modelId", e.getKey());
            r.put("modelName", m == null ? "Model#" + e.getKey() : m.getName());
            r.put("provider", m == null ? "?" : m.getProvider());
            r.put("calls", list.size());
            r.put("inputTokens", inTok);
            r.put("outputTokens", outTok);
            r.put("totalTokens", inTok + outTok);
            r.put("cost", cost);
            r.put("avgLatencyMs", round(avgLat, 0));
            result.add(r);
        }
        result.sort((x, y) -> ((BigDecimal) y.get("cost")).compareTo((BigDecimal) x.get("cost")));
        return result;
    }

    @Override
    public Map<String, Object> platformSummary() {
        // 跨评测统计
        List<Answer> all = answerMapper.selectList(null);
        if (all.isEmpty()) {
            return Map.of("totalCalls", 0, "totalCost", BigDecimal.ZERO);
        }
        BigDecimal cost = all.stream()
                .filter(a -> a.getEstimatedCost() != null)
                .map(Answer::getEstimatedCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);
        long tokens = all.stream().filter(a -> a.getTokenInput() != null).mapToLong(Answer::getTokenInput).sum()
                + all.stream().filter(a -> a.getTokenOutput() != null).mapToLong(Answer::getTokenOutput).sum();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("totalCalls", all.size());
        r.put("totalTokens", tokens);
        r.put("totalCost", cost);
        return r;
    }

    @Override
    public byte[] exportCsv(Long evaluationId) {
        Evaluation e = evaluationMapper.selectById(evaluationId);
        if (e == null) throw new BusinessException(ErrorCode.EVALUATION_NOT_FOUND);

        List<Answer> answers = answerMapper.selectList(
                new LambdaQueryWrapper<Answer>().eq(Answer::getEvaluationId, evaluationId));
        Set<Long> modelIds = answers.stream().map(Answer::getModelId).collect(Collectors.toSet());
        List<ModelConfig> models = modelConfigMapper.selectBatchIds(modelIds);
        Map<Long, ModelConfig> modelMap = new HashMap<>();
        for (ModelConfig m : models) modelMap.put(m.getId(), m);

        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF'); // UTF-8 BOM
        sb.append("answer_id,question_id,model,provider,status,input_tokens,output_tokens,latency_ms,estimated_cost,error_code,created_at\n");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (Answer a : answers) {
            ModelConfig m = modelMap.get(a.getModelId());
            String mName = m == null ? "Model#" + a.getModelId() : m.getName().replace(",", " ");
            String prov = m == null ? "" : m.getProvider();
            String status = a.getErrorCode() == null ? "OK" : "FAIL";
            sb.append(a.getId()).append(',')
              .append(a.getQuestionId()).append(',')
              .append(mName).append(',')
              .append(prov).append(',')
              .append(status).append(',')
              .append(a.getTokenInput() == null ? 0 : a.getTokenInput()).append(',')
              .append(a.getTokenOutput() == null ? 0 : a.getTokenOutput()).append(',')
              .append(a.getLatencyMs() == null ? 0 : a.getLatencyMs()).append(',')
              .append(a.getEstimatedCost() == null ? "0" : a.getEstimatedCost().toPlainString()).append(',')
              .append(a.getErrorCode() == null ? "" : a.getErrorCode()).append(',')
              .append(a.getCreatedAt() == null ? "" : a.getCreatedAt().format(fmt))
              .append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static double round(double v, int scale) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return 0;
        return BigDecimal.valueOf(v).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }

    private static class Bucket {
        long calls = 0;
        BigDecimal cost = BigDecimal.ZERO;
    }
}
