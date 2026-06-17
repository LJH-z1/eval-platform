# FR-07 成本与耗时统计 — TODO

> **负责人:梁倩倩**
> **关联分支**:`feature/eval`
> **预计工作量**:5 人日
> **对应评分层**:基础层 + 提高层

## 一、任务清单

### 1.1 Service(1.5 天)

- [ ] `BillingService.summary` — 汇总某评测的总调用、总 token、总费用、平均耗时
- [ ] `BillingService.timeSeries` — 按 hour/day 粒度的折线图数据
- [ ] `BillingService.byModel` — 各模型对比柱状图数据
- [ ] `BillingService.exportCsv` — 导出 CSV(用 StringBuilder 拼即可,不需要 EasyExcel)

### 1.2 Controller(0.5 天)

- [ ] `/api/billing/export` 用 `ResponseEntity<byte[]>` + `Content-Disposition: attachment`

### 1.3 数据库(0.5 天)

- [ ] 复用 answer 表的 token_input / token_output / latency_ms / estimated_cost 字段,**不需要新表**

### 1.4 单元测试(1 天) — 覆盖 TC-07-001 ~ 005

- [ ] Token 统计
- [ ] 耗时统计
- [ ] 费用估算
- [ ] 图表展示
- [ ] CSV 导出

## 二、数据源

- `answer.token_input` / `answer.token_output`(FR-04 写入)
- `answer.latency_ms`(FR-04 写入)
- `answer.estimated_cost = (token_input + token_output) / 1000 * model.pricePerK`(FR-04 写入)

## 三、ECharts 数据格式

```json
{
  "xAxis": ["10:00", "11:00", "12:00"],
  "series": [
    {"name": "调用量", "type": "line", "data": [10, 25, 18]},
    {"name": "费用", "type": "line", "data": [0.05, 0.12, 0.09]}
  ]
}
```

## 四、参考资料

- 完整参考:`eval-platform-reference/backend/src/main/java/com/mavis/evalplatform/billing/`
- 需求:§3.7
- 架构:§4.2.7
