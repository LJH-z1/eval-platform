package com.mavis.evalplatform.dashboard.service;

/**
 * 首页统计聚合服务 — 兼容旧版前端 /api/dashboard/* 端点
 *
 * @author 刘家豪
 */
public interface DashboardService {
    /** 总览统计:题库 / 模型 / 评测 / 评分 / 平台总成本 */
    java.util.Map<String, Object> stats();
    /** 最近活动:最近 10 条评测 */
    java.util.List<java.util.Map<String, Object>> recent(int limit);
}
