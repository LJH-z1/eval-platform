package com.mavis.evalplatform.export.service.impl;

import com.mavis.evalplatform.billing.service.BillingService;
import com.mavis.evalplatform.common.exception.BusinessException;
import com.mavis.evalplatform.common.exception.ErrorCode;
import com.mavis.evalplatform.evaluation.entity.Answer;
import com.mavis.evalplatform.evaluation.entity.Evaluation;
import com.mavis.evalplatform.evaluation.mapper.AnswerMapper;
import com.mavis.evalplatform.evaluation.mapper.EvaluationMapper;
import com.mavis.evalplatform.export.service.ReportExportService;
import com.mavis.evalplatform.model.entity.ModelConfig;
import com.mavis.evalplatform.model.mapper.ModelConfigMapper;
import com.mavis.evalplatform.score.entity.Score;
import com.mavis.evalplatform.score.mapper.ScoreMapper;
import com.mavis.evalplatform.stats.service.FleissKappaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 报告导出实现(FR-08) — 简化版
 * <p>
 * 不依赖 EasyExcel / iText,直接用纯 Java 生成 CSV + HTML。
 * Excel 可直接打开 CSV,HTML 浏览器/Word 可查看。
 *
 * @author 周文泽
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportExportServiceImpl implements ReportExportService {

    private final EvaluationMapper evaluationMapper;
    private final AnswerMapper answerMapper;
    private final ScoreMapper scoreMapper;
    private final ModelConfigMapper modelConfigMapper;
    private final BillingService billingService;
    private final FleissKappaService fleissKappaService;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public ByteArrayOutputStream exportCsv(Long evaluationId) {
        Evaluation e = loadOrThrow(evaluationId);
        List<Answer> answers = answerMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Answer>()
                        .eq(Answer::getEvaluationId, evaluationId));
        Set<Long> answerIds = answers.stream().map(Answer::getId).collect(Collectors.toSet());
        Set<Long> modelIds = answers.stream().map(Answer::getModelId).collect(Collectors.toSet());
        List<ModelConfig> models = modelConfigMapper.selectBatchIds(modelIds);
        Map<Long, ModelConfig> modelMap = new HashMap<>();
        for (ModelConfig m : models) modelMap.put(m.getId(), m);

        List<Score> scores = scoreMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Score>()
                        .in(Score::getAnswerId, answerIds));
        Map<Long, List<Score>> scoreByAns = new HashMap<>();
        for (Score s : scores) scoreByAns.computeIfAbsent(s.getAnswerId(), k -> new ArrayList<>()).add(s);

        Map<String, Object> summary = billingService.summary(evaluationId);
        List<Map<String, Object>> byModel = billingService.byModel(evaluationId);
        Map<String, Object> kappa = fleissKappaService.getKappa(evaluationId);

        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF');
        sb.append("# 多模型对比评测报告\r\n");
        sb.append("# 评测 ID:").append(evaluationId).append(" · 名称:").append(e.getName()).append("\r\n");
        sb.append("# 状态:").append(e.getStatus()).append(" · 创建时间:").append(formatDateTime(e.getCreatedAt())).append("\r\n");
        sb.append("\r\n=== 总览 ===\r\n");
        appendKVSB(sb, summary);
        sb.append("\r\n=== 评分一致性(Fleiss Kappa) ===\r\n");
        @SuppressWarnings("unchecked")
        Map<String, Double> kappas = (Map<String, Double>) kappa.get("kappas");
        if (kappas != null) {
            kappas.forEach((k, v) -> sb.append(k).append(",").append(v == null ? "N/A" : v).append("\r\n"));
        }
        sb.append("\r\n=== 各模型成本 ===\r\n");
        sb.append("model,provider,calls,input_tokens,output_tokens,total_cost,avg_latency_ms\r\n");
        for (Map<String, Object> r : byModel) {
            sb.append(safeCsv(r.get("modelName"))).append(',')
              .append(safeCsv(r.get("provider"))).append(',')
              .append(r.get("calls")).append(',')
              .append(r.get("inputTokens")).append(',')
              .append(r.get("outputTokens")).append(',')
              .append(r.get("cost")).append(',')
              .append(r.get("avgLatencyMs")).append("\r\n");
        }
        sb.append("\r\n=== 详细结果(每行 = 一个 answer) ===\r\n");
        sb.append("answer_id,question_id,model,provider,status,input_tokens,output_tokens,latency_ms,cost,avg_score,scorer_count,created_at\r\n");
        for (Answer a : answers) {
            ModelConfig m = modelMap.get(a.getModelId());
            String mName = m == null ? "Model#" + a.getModelId() : m.getName();
            String prov = m == null ? "" : m.getProvider();
            String status = a.getErrorCode() == null ? "OK" : "FAIL";
            List<Score> ss = scoreByAns.getOrDefault(a.getId(), Collections.emptyList());
            double avg = ss.isEmpty() ? 0 : ss.stream().mapToDouble(s -> (s.getAccuracy() + s.getRelevance() + s.getFluency() + s.getSafety()) / 4.0).average().orElse(0);
            sb.append(a.getId()).append(',')
              .append(a.getQuestionId()).append(',')
              .append(safeCsv(mName)).append(',')
              .append(safeCsv(prov)).append(',')
              .append(status).append(',')
              .append(a.getTokenInput() == null ? 0 : a.getTokenInput()).append(',')
              .append(a.getTokenOutput() == null ? 0 : a.getTokenOutput()).append(',')
              .append(a.getLatencyMs() == null ? 0 : a.getLatencyMs()).append(',')
              .append(a.getEstimatedCost() == null ? 0 : a.getEstimatedCost().toPlainString()).append(',')
              .append(round(avg, 2)).append(',')
              .append(ss.size()).append(',')
              .append(formatDateTime(a.getCreatedAt()))
              .append("\r\n");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e2) { /* ignore */ }
        return out;
    }

    @Override
    public ByteArrayOutputStream exportHtml(Long evaluationId) {
        Evaluation e = loadOrThrow(evaluationId);
        List<Answer> answers = answerMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Answer>()
                        .eq(Answer::getEvaluationId, evaluationId));
        Set<Long> modelIds = answers.stream().map(Answer::getModelId).collect(Collectors.toSet());
        List<ModelConfig> models = modelConfigMapper.selectBatchIds(modelIds);
        Map<Long, ModelConfig> modelMap = new HashMap<>();
        for (ModelConfig m : models) modelMap.put(m.getId(), m);

        Map<String, Object> summary = billingService.summary(evaluationId);
        List<Map<String, Object>> byModel = billingService.byModel(evaluationId);
        Map<String, Object> kappa = fleissKappaService.getKappa(evaluationId);
        List<Map<String, Object>> modelRank = fleissKappaService.getModelRanking(evaluationId);

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html lang=\"zh\"><head><meta charset=\"utf-8\"><title>评测报告 #")
          .append(evaluationId).append("</title>");
        sb.append("<style>")
          .append("body{font-family:'Microsoft YaHei',Arial,sans-serif;max-width:1100px;margin:30px auto;padding:0 30px;color:#1e293b;line-height:1.7}")
          .append("h1{font-size:28px;border-bottom:3px solid #2563eb;padding-bottom:10px;margin-bottom:24px}")
          .append("h2{font-size:20px;color:#2563eb;margin:32px 0 12px;padding-left:10px;border-left:4px solid #2563eb}")
          .append("h3{font-size:16px;color:#475569;margin:20px 0 8px}")
          .append(".meta{color:#94a3b8;font-size:13px;margin-bottom:24px}")
          .append("table{border-collapse:collapse;width:100%;margin:12px 0;font-size:13px}")
          .append("th,td{border:1px solid #e2e8f0;padding:8px 12px;text-align:left}")
          .append("th{background:#eff6ff;color:#1e40af;font-weight:600}")
          .append("tr:nth-child(even) td{background:#f8fafc}")
          .append(".badge{display:inline-block;padding:2px 8px;border-radius:999px;font-size:11px;font-weight:600}")
          .append(".b-ok{background:#dcfce7;color:#15803d}")
          .append(".b-fail{background:#fee2e2;color:#dc2626}")
          .append(".b-mid{background:#dbeafe;color:#1e40af}")
          .append(".stat{display:grid;grid-template-columns:repeat(4,1fr);gap:12px;margin:16px 0}")
          .append(".stat>div{background:#f8fafc;padding:14px;border-radius:8px;border:1px solid #e2e8f0}")
          .append(".stat .n{font-size:22px;font-weight:800;color:#2563eb}")
          .append(".stat .l{font-size:12px;color:#94a3b8}")
          .append(".kpi{background:#eff6ff;padding:20px;border-radius:10px;margin:16px 0;border:1px solid #dbeafe}")
          .append(".kpi-big{font-size:32px;font-weight:800;color:#2563eb}")
          .append("</style></head><body>");

        sb.append("<h1>📊 多模型对比评测报告</h1>");
        sb.append("<div class='meta'>评测 ID:").append(evaluationId)
          .append(" · 状态:").append("<span class='badge b-" + ("COMPLETED".equals(e.getStatus()) ? "ok" : "mid") + "'>").append(e.getStatus()).append("</span>")
          .append(" · 创建时间:").append(formatDateTime(e.getCreatedAt())).append("</div>");

        sb.append("<h2>📈 第一章 评测总览</h2>");
        sb.append("<div class='stat'>");
        appendStat(sb, "总调用", String.valueOf(summary.get("totalCalls")));
        appendStat(sb, "总 Token", String.valueOf(summary.get("totalTokens")));
        appendStat(sb, "总费用", "¥" + summary.get("totalCost"));
        appendStat(sb, "平均耗时", summary.get("avgLatencyMs") + " ms");
        sb.append("</div>");

        sb.append("<h2>🎯 第二章 评分一致性(Fleiss Kappa)</h2>");
        @SuppressWarnings("unchecked")
        Map<String, Object> kappas = (Map<String, Object>) kappa.get("kappas");
        if (kappas != null) {
            sb.append("<table><tr><th>维度</th><th>Kappa</th><th>解读</th></tr>");
            kappas.forEach((k, v) -> {
                String val = v == null ? "N/A" : String.valueOf(v);
                sb.append("<tr><td>").append(k).append("</td><td>").append(val).append("</td><td>")
                  .append(kappa.get("interpretation")).append("</td></tr>");
            });
            sb.append("</table>");
        }

        sb.append("<h2>💰 第三章 各模型成本</h2>");
        sb.append("<table><tr><th>模型</th><th>提供商</th><th>调用数</th><th>Token</th><th>费用</th><th>平均耗时</th></tr>");
        for (Map<String, Object> r : byModel) {
            sb.append("<tr><td>").append(safeHtml(String.valueOf(r.get("modelName")))).append("</td>")
              .append("<td>").append(safeHtml(String.valueOf(r.get("provider")))).append("</td>")
              .append("<td>").append(r.get("calls")).append("</td>")
              .append("<td>").append(r.get("totalTokens")).append("</td>")
              .append("<td>¥").append(r.get("cost")).append("</td>")
              .append("<td>").append(r.get("avgLatencyMs")).append(" ms</td></tr>");
        }
        sb.append("</table>");

        sb.append("<h2>🏆 第四章 模型排名(加权总分)</h2>");
        sb.append("<table><tr><th>排名</th><th>模型</th><th>准确性</th><th>相关性</th><th>流畅性</th><th>安全性</th><th>加权总分</th></tr>");
        for (int i = 0; i < modelRank.size(); i++) {
            Map<String, Object> r = modelRank.get(i);
            sb.append("<tr><td>🥇 第 ").append(i + 1).append(" 名</td>")
              .append("<td><strong>").append(safeHtml(String.valueOf(r.get("modelName")))).append("</strong></td>")
              .append("<td>").append(r.get("accuracy")).append("</td>")
              .append("<td>").append(r.get("relevance")).append("</td>")
              .append("<td>").append(r.get("fluency")).append("</td>")
              .append("<td>").append(r.get("safety")).append("</td>")
              .append("<td><strong style='color:#2563eb'>").append(r.get("weightedScore")).append("</strong></td></tr>");
        }
        sb.append("</table>");

        sb.append("<h2>📝 第五章 详细结果</h2>");
        sb.append("<table><tr><th>问题</th><th>模型</th><th>状态</th><th>得分(avg)</th><th>延迟</th><th>费用</th></tr>");
        // 简化按问题+模型分组的展示 — 太多行了截断
        int max = Math.min(answers.size(), 50);
        for (int i = 0; i < max; i++) {
            Answer a = answers.get(i);
            ModelConfig m = modelMap.get(a.getModelId());
            sb.append("<tr><td>Q").append(a.getQuestionId()).append("</td>")
              .append("<td>").append(safeHtml(m == null ? "?" : m.getName())).append("</td>")
              .append("<td>").append(a.getErrorCode() == null ? "<span class='badge b-ok'>OK</span>" : "<span class='badge b-fail'>FAIL</span>").append("</td>")
              .append("<td>—</td>")
              .append("<td>").append(a.getLatencyMs() == null ? 0 : a.getLatencyMs()).append(" ms</td>")
              .append("<td>¥").append(a.getEstimatedCost() == null ? "0" : a.getEstimatedCost().toPlainString()).append("</td></tr>");
        }
        if (answers.size() > max) {
            sb.append("<tr><td colspan='6' style='text-align:center;color:#94a3b8'>… 仅展示前 ").append(max).append(" / ").append(answers.size()).append(" 条</td></tr>");
        }
        sb.append("</table>");

        sb.append("<h2>📌 第六章 报告说明</h2>");
        sb.append("<p style='color:#94a3b8;font-size:12px'>本报告由 EvalArena 自动生成 · 加权总分 = 准确性×0.4 + 相关性×0.3 + 流畅性×0.2 + 安全性×0.1 · ")
          .append("Fleiss Kappa 解读:&lt;0 差 / 0-0.2 轻微 / 0.2-0.4 一般 / 0.4-0.6 中等 / 0.6-0.8 良好 / &gt;0.8 优秀</p>");
        sb.append("</body></html>");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) { /* ignore */ }
        return out;
    }

    @Override
    public Object reportMeta(Long evaluationId) {
        Evaluation e = loadOrThrow(evaluationId);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", e.getId());
        r.put("name", e.getName());
        r.put("status", e.getStatus());
        r.put("createdAt", e.getCreatedAt());
        return r;
    }

    // ---- 工具 ----

    private Evaluation loadOrThrow(Long id) {
        Evaluation e = evaluationMapper.selectById(id);
        if (e == null) throw new BusinessException(ErrorCode.EVALUATION_NOT_FOUND);
        return e;
    }

    private void appendKVSB(StringBuilder sb, Map<String, Object> map) {
        map.forEach((k, v) -> sb.append(k).append(",").append(v == null ? "" : v).append("\r\n"));
    }

    private void appendStat(StringBuilder sb, String label, String value) {
        sb.append("<div><div class='n'>").append(value).append("</div><div class='l'>").append(label).append("</div></div>");
    }

    private static String safeCsv(Object v) {
        if (v == null) return "";
        String s = v.toString();
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private static String safeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String formatDateTime(Object o) {
        if (o == null) return "";
        if (o instanceof java.time.LocalDateTime) return ((java.time.LocalDateTime) o).format(FMT);
        return o.toString();
    }

    private static double round(double v, int scale) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return 0;
        return BigDecimal.valueOf(v).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }
}
