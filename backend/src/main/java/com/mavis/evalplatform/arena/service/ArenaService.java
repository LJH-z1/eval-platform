package com.mavis.evalplatform.arena.service;

import java.util.List;
import java.util.Map;

/**
 * Arena 盲评 + Elo 排名 Service
 *
 * @author 刘家豪
 */
public interface ArenaService {

    /** 快速评测:同步跑 1 题 2 模型,返回两边回答 */
    Map<String, Object> quickEvaluate(String prompt, Long modelAId, Long modelBId, Long userId);

    /** 批量评测:同步跑 N 题 2 模型,返回每题的两边回答(parallel) */
    List<Map<String, Object>> batchEvaluate(List<String> prompts, Long modelAId, Long modelBId, Long userId);

    /** 投票并落库 */
    Long vote(Long evaluationId, String prompt, Long leftModelId, Long rightModelId, String winner, Long userId);

    /** Elo 排行榜(全量重算) */
    List<Map<String, Object>> ranking();
}
