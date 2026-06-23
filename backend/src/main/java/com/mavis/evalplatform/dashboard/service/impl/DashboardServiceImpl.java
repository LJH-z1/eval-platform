package com.mavis.evalplatform.dashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mavis.evalplatform.dashboard.service.DashboardService;
import com.mavis.evalplatform.evaluation.entity.Answer;
import com.mavis.evalplatform.evaluation.entity.Evaluation;
import com.mavis.evalplatform.evaluation.mapper.AnswerMapper;
import com.mavis.evalplatform.evaluation.mapper.EvaluationMapper;
import com.mavis.evalplatform.model.entity.ModelConfig;
import com.mavis.evalplatform.model.mapper.ModelConfigMapper;
import com.mavis.evalplatform.question.entity.Question;
import com.mavis.evalplatform.question.mapper.QuestionMapper;
import com.mavis.evalplatform.score.entity.Score;
import com.mavis.evalplatform.score.mapper.ScoreMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 首页统计聚合 — 复用各模块的 mapper,避免新建视图
 *
 * @author 刘家豪
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final QuestionMapper questionMapper;
    private final ModelConfigMapper modelConfigMapper;
    private final EvaluationMapper evaluationMapper;
    private final AnswerMapper answerMapper;
    private final ScoreMapper scoreMapper;

    @Override
    public Map<String, Object> stats() {
        Map<String, Object> m = new HashMap<>();
        m.put("questions",   questionMapper.selectCount(null));
        m.put("models",      modelConfigMapper.selectCount(null));
        m.put("evaluations", evaluationMapper.selectCount(null));
        m.put("answers",     answerMapper.selectCount(null));
        m.put("scores",      scoreMapper.selectCount(null));
        // 复用 Billing 平台的"总成本/总调用" 字段;若无则给 0
        m.put("totalCost", 0.0);
        m.put("totalCalls", answerMapper.selectCount(null));
        return m;
    }

    @Override
    public List<Map<String, Object>> recent(int limit) {
        List<Evaluation> list = evaluationMapper.selectList(
            new LambdaQueryWrapper<Evaluation>()
                .orderByDesc(Evaluation::getCreatedAt)
                .last("LIMIT " + Math.max(1, Math.min(limit, 50)))
        );
        List<Map<String, Object>> out = new java.util.ArrayList<>();
        for (Evaluation e : list) {
            Map<String, Object> r = new HashMap<>();
            r.put("id", e.getId());
            r.put("name", e.getName());
            r.put("status", e.getStatus());
            r.put("createdAt", e.getCreatedAt());
            // 该评测的答案数 / 已评分数
            Long answers = answerMapper.selectCount(
                new LambdaQueryWrapper<Answer>().eq(Answer::getEvaluationId, e.getId()));
            r.put("answerCount", answers);
            out.add(r);
        }
        return out;
    }
}
