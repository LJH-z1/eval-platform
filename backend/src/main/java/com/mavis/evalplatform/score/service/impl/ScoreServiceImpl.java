package com.mavis.evalplatform.score.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mavis.evalplatform.common.exception.BusinessException;
import com.mavis.evalplatform.common.exception.ErrorCode;
import com.mavis.evalplatform.score.dto.ScoreRequest;
import com.mavis.evalplatform.score.entity.Score;
import com.mavis.evalplatform.score.mapper.ScoreMapper;
import com.mavis.evalplatform.score.service.ScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评分 Service 实现(FR-05)
 *
 * @author 宋子翔
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScoreServiceImpl implements ScoreService {

    private final ScoreMapper scoreMapper;

    @Override
    public Score submit(ScoreRequest req, Long scorerId) {
        // 1) 校验(双重防御 — annotation 也会校验)
        validate(req);

        // 2) 检查是否已评过(UNIQUE 约束)
        Score exist = findByAnswerAndScorer(req.getAnswerId(), scorerId);
        if (exist != null) {
            throw new BusinessException(ErrorCode.SCORE_ALREADY_SUBMITTED);
        }

        // 3) 写
        Score s = new Score();
        s.setAnswerId(req.getAnswerId());
        s.setScorerId(scorerId);
        s.setAccuracy(req.getAccuracy());
        s.setRelevance(req.getRelevance());
        s.setFluency(req.getFluency());
        s.setSafety(req.getSafety());
        s.setComment(req.getComment());
        s.setCreatedAt(LocalDateTime.now());
        try {
            scoreMapper.insert(s);
        } catch (DuplicateKeyException dke) {
            // 并发场景兜底
            throw new BusinessException(ErrorCode.SCORE_ALREADY_SUBMITTED);
        }
        log.info("[Score] submit answerId={} scorer={} acc={} rel={} flu={} saf={}",
                req.getAnswerId(), scorerId, req.getAccuracy(), req.getRelevance(), req.getFluency(), req.getSafety());
        return s;
    }

    @Override
    public Score submit(Long answerId, Long scorerId, Integer accuracy, Integer relevance,
                        Integer fluency, Integer safety, String comment) {
        ScoreRequest req = new ScoreRequest();
        req.setAnswerId(answerId);
        req.setAccuracy(accuracy);
        req.setRelevance(relevance);
        req.setFluency(fluency);
        req.setSafety(safety);
        req.setComment(comment);
        return submit(req, scorerId);
    }

    @Override
    public List<Score> listByScorerAndEvaluation(Long scorerId, Long evaluationId) {
        // 通过 answerId IN (...) 过滤
        // 简化:用 SQL 联表
        return scoreMapper.selectList(
                new LambdaQueryWrapper<Score>()
                        .eq(Score::getScorerId, scorerId)
                        .orderByDesc(Score::getCreatedAt));
    }

    @Override
    public List<Score> listByAnswer(Long answerId) {
        return scoreMapper.selectList(
                new LambdaQueryWrapper<Score>()
                        .eq(Score::getAnswerId, answerId));
    }

    public Score findByAnswerAndScorer(Long answerId, Long scorerId) {
        return scoreMapper.selectOne(
                new LambdaQueryWrapper<Score>()
                        .eq(Score::getAnswerId, answerId)
                        .eq(Score::getScorerId, scorerId)
                        .last("LIMIT 1"));
    }

    /**
     * 进度统计 — 已评 / 总数
     */
    public int countByScorerAndEvaluation(Long scorerId, Long evaluationId, List<Long> answerIds) {
        if (answerIds == null || answerIds.isEmpty()) return 0;
        return Math.toIntExact(scoreMapper.selectCount(
                new LambdaQueryWrapper<Score>()
                        .eq(Score::getScorerId, scorerId)
                        .in(Score::getAnswerId, answerIds)));
    }

    private void validate(ScoreRequest req) {
        if (req.getAnswerId() == null) {
            throw new BusinessException(ErrorCode.SCORE_ANSWER_NOT_FOUND);
        }
        validateScore("accuracy",  req.getAccuracy());
        validateScore("relevance", req.getRelevance());
        validateScore("fluency",   req.getFluency());
        validateScore("safety",    req.getSafety());
        if (StringUtils.hasText(req.getComment()) && req.getComment().length() > 500) {
            throw new BusinessException(ErrorCode.SCORE_COMMENT_TOO_LONG);
        }
    }

    private void validateScore(String field, Integer v) {
        if (v == null || v < 1 || v > 5) {
            throw new BusinessException(ErrorCode.SCORE_SCORE_OUT_OF_RANGE, field + " 必须在 1-5 之间");
        }
    }
}
