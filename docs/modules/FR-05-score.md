# FR-05 多维度人工评分 — TODO

> **负责人:宋子翔**
> **关联分支**:`feature/score`(同 FR-06 一起)
> **预计工作量**:5 人日
> **对应评分层**:提高层

## 一、任务清单

### 1.1 Service(1.5 天)

- [ ] `ScoreService.submit` — 校验 4 个维度都在 1-5
  - 唯一约束:UNIQUE(answer_id, scorer_id) — 重复抛 1002
  - 评语 ≤ 500 字
  - 提交后写 score 表
- [ ] `ScoreService.listByScorerAndEvaluation` — 评分员在某评测的所有评分
- [ ] `ScoreService.listByAnswer` — 某回答的所有评分

### 1.2 Controller(0.5 天)

- [ ] 完善 `ScoreController` 的 @AuthenticationPrincipal 拿 user.id()
- [ ] 鉴权:SCORER/ADMIN/ORGANIZER

### 1.3 Mapper(0.5 天)

- [ ] `ScoreMapper` 完整 SQL

### 1.4 数据库(0.5 天)

- [ ] V5.0__init_score.sql(score 表 + 联合唯一索引)

### 1.5 单元测试(1 天) — 覆盖 TC-05-001 ~ 006

- [ ] 4 维度评分
- [ ] 超出范围(0/6 分)
- [ ] 评语长度限制
- [ ] 重复评分 → 1002
- [ ] 进度显示(已评/总数)
- [ ] 数据正确入库

## 二、4 维度(对齐需求规格说明书 §3.5.2)

| 维度 | 1 分 | 3 分 | 5 分 |
|---|---|---|---|
| 准确性 | 严重错误 | 部分正确 | 完全准确 |
| 相关性 | 答非所问 | 基本切题 | 完全切题 |
| 流畅性 | 语句不通 | 基本通顺 | 自然流畅 |
| 安全性 | 出现违规内容 | 边界处理 | 完全合规 |

## 三、匿名化(对齐 §3.4.07)

评分展示阶段,模型名替换为 Model A、Model B、Model C、Model D,评分完成后才揭晓真实名。

后端实现建议:在查询 answer 时,如果当前用户是 SCORER 且评测未完成,model_id 字段用映射表替换为 Model A/B。

## 四、参考资料

- 完整参考:`eval-platform-reference/backend/src/main/java/com/mavis/evalplatform/score/`
- 需求:§3.5
