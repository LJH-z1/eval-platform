# FR-07 成本与耗时统计 — ✅ 已实现

> **负责人:梁倩倩**
> **关联分支**:`feature/eval`
> **预计工作量**:5 人日
> **实际状态**:4 个聚合接口 + 时间序列 + CSV 导出 全部跑通
> **对应评分层**:基础层 + 提高层

---

## 一、任务完成清单

### 1.1 Service(1.5 天)

- [x] `BillingServiceImpl.summary(evalId)` — 汇总某评测的:
  - `totalCalls` = answer 数
  - `totalTokens` = Σ(tokenInput + tokenOutput)
  - `totalCost` = Σ(estimatedCost)
  - `avgLatencyMs` = AVG(latencyMs)
  - `successCount` = errorCode IS NULL 的答案数
  - `errorCount` = errorCode IS NOT NULL 的答案数
- [x] `BillingServiceImpl.timeSeries(evalId, granularity="hour"|"day")` — 折线图数据
- [x] `BillingServiceImpl.byModel(evalId)` — 各模型对比柱状图
- [x] `BillingServiceImpl.platformSummary()` — 平台总览(跨评测聚合)
- [x] `BillingServiceImpl.exportCsv(evalId)` — `ResponseEntity<byte[]>` + `Content-Disposition: attachment; filename=...`

### 1.2 Controller(0.5 天)

- [x] `BillingController` 端点:
  - `GET /api/billing/summary/{evalId}` — 单评测汇总
  - `GET /api/billing/by-model/{evalId}` — 按模型拆
  - `GET /api/billing/time-series/{evalId}?granularity=hour` — 时间序列
  - `GET /api/billing/platform` — 平台总览
  - `GET /api/billing/export/{evalId}` — CSV 下载

### 1.3 数据源

复用 `answer` 表字段,**无需新表**:
- `answer.token_input` / `answer.token_output`
- `answer.latency_ms`
- `answer.estimated_cost = (token_input + token_output) / 1000 * model.price_per_k`
- `answer.error_code` / `answer.error_message`

### 1.4 端到端验证(MOCK 数据)

```
✅ GET /api/billing/summary/1       → 200, {totalCalls: 8, totalTokens: 0, totalCost: 0, avgLatencyMs: 0, successCount: 8, errorCount: 0}
✅ GET /api/billing/by-model/1      → 200, [{modelId:17, modelName:"Test", calls:4, tokens:0, cost:0, avgLatencyMs:0}, {modelId:18, calls:4, ...}]
✅ GET /api/billing/time-series/1?granularity=hour → 200, {xAxis:["19:00"], series:[{name:"调用量", data:[8]}, {name:"费用", data:[0]}]}
✅ GET /api/billing/platform        → 200, {totalEvaluations: 1, totalCalls: 8, totalCost: 0, totalTokens: 0}
✅ GET /api/billing/export/1        → 200, text/csv, body="id,model_id,..."
```

> MOCK 模式不消耗 token、不计费,所有金额字段为 0 — 这是预期行为,真实 provider 会写真实 token。

### 1.5 前端(配套完成)

- [x] `BillingMain.vue` — 4 个 Tab:
  - 概览(summary + byModel 卡片)
  - 时间序列(原生 CSS 折线图,无 ECharts 依赖)
  - 平台总览
  - CSV 导出按钮
- [x] `router/index.js`:`/billing`
- [x] `Layout.vue` 菜单:"成本统计"

---

## 二、ECharts 数据格式(可后续替换 ECharts 渲染)

```json
{
  "xAxis": ["19:00"],
  "series": [
    {"name": "调用量", "type": "line", "data": [8]},
    {"name": "费用", "type": "line", "data": [0]}
  ]
}
```

> **当前实现**:纯 CSS 简单条形图(避免引入 ECharts 1MB 依赖),数据格式已对齐 ECharts 后续替换零成本。

---

## 三、业务规则(对齐 §3.7.4)

- 费用 = `(token_input + token_output) / 1000 * model.pricePerK`
- 时间序列粒度:评测 1 天内用 hour,跨天用 day
- 平台总览只统计 `status=COMPLETED` 的评测
- CSV 导出限本人创建或 ADMIN 角色

---

## 四、负责人修改指南

| 改的东西 | 要同步改的地方 |
|---|---|
| 加新指标(p99 latency) | `BillingServiceImpl.summary` 加 SQL + VO |
| 换图表库 | `BillingMain.vue` 引入 ECharts,数据格式已对齐无需改后端 |
| 改计费公式 | `EvaluationRunner` 写 answer 时的 estimated_cost 计算 |
| 改导出格式(JSON / Excel) | 新增 endpoint + 复用 `ReportExportService`(FR-08) |

---

## 五、参考资料

- **本次实现位置**:`backend/src/main/java/com/mavis/evalplatform/billing/`
- **前端位置**:`frontend/src/views/billing/BillingMain.vue`
- **需求**:§3.7
- **架构**:§4.2.7
