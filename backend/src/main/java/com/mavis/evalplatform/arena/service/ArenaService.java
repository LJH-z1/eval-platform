package com.mavis.evalplatform.arena.service;

import java.util.List;
import java.util.Map;

/**
 * Arena 盲评 + Elo 排名 Service
 *
 * @author 刘家豪
 */
public interface ArenaService {

    /** 快速评测:同步跑 1 题 2 模型,返回两边回答(category 可空,默认 "general") */
    Map<String, Object> quickEvaluate(String prompt, Long modelAId, Long modelBId, String category, Long userId);

    /** 批量评测:同步跑 N 题 2 模型,返回每题的两边回答(parallel) */
    List<Map<String, Object>> batchEvaluate(List<String> prompts, Long modelAId, Long modelBId, String category, Long userId);

    /** 投票并落库(带 category) */
    Long vote(Long evaluationId, String prompt, Long leftModelId, Long rightModelId, String winner, String category, Long userId);

    /** Elo 排行榜(全量重算) */
    List<Map<String, Object>> ranking();

    /** 按能力分类的 Elo 排行榜(category="all" 或空 = 总榜,否则按分类) */
    List<Map<String, Object>> rankingByCategory(String category);
}
