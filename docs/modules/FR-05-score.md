# FR-05 多维度人工评分 — ✅ 已实现

> **负责人:宋子翔**
> **关联分支**:`feature/score`(同 FR-06 一起)
> **预计工作量**:5 人日
> **实际状态**:后端 + 前端 + E2E 全部跑通(MOCK 数据 4 评分 × 4 维度)
> **对应评分层**:提高层

---

## 一、任务完成清单

### 1.1 Service(1.5 天)

- [x] `ScoreServiceImpl.submit`:
  - 4 维度分值校验 `1 ≤ dim ≤ 5`,越界抛 `1022`
  - 唯一约束:`UNIQUE(answer_id, scorer_id)`,重复抛 `1041`
  - 评语 ≤ 500 字
  - 提交后 `INSERT score`,回填 `id`
- [x] `ScoreServiceImpl.listByScorerAndEvaluation(scorerId, evalId)` — 评分员在某评测的所有评分
- [x] `ScoreServiceImpl.listByAnswer(answerId)` — 某回答的所有评分
- [x] `ScoreServiceImpl.checkScored(answerId, scorerId)` — 防重复点击

### 1.2 Controller(0.5 天)

- [x] `ScoreController` 用 `@AuthenticationPrincipal AuthenticatedUser` 拿 `userId`(不再走 JWT 二次解析)
- [x] 端点:
  - `POST /api/scores` — 提交评分(SCORER/ADMIN/ORGANIZER)
  - `GET /api/scores/by-answer/{answerId}` — 某回答的评分列表
  - `GET /api/scores/by-evaluation/{evalId}/by-scorer/{scorerId}` — 评分员在评测的所有评分
  - `GET /api/scores/check/{answerId}` — 当前用户是否已评

### 1.3 Mapper(0.5 天)

- [x] `ScoreMapper extends BaseMapper<Score>`,无自定义 SQL

### 1.4 数据库(0.5 天)

- [x] `V1.0__init.sql` 中建表 `score`:
  - 字段:`id, answer_id, scorer_id, accuracy, relevance, fluency, safety, comment, created_at`
  - 索引:`UNIQUE(answer_id, scorer_id)`,`idx_answer`,`idx_scorer`
  - 4 维度分值均为 `TINYINT NOT NULL CHECK(1 <= dim <= 5)`

### 1.5 端到端验证(已在 MySQL prod 跑通)

```
✅ POST /api/scores (4 维度全 5)           → 200, id=1
✅ POST /api/scores (重复同 answer)        → 200, code=1041, msg=已评分
✅ POST /api/scores (accuracy=0 越界)      → 200, code=1022, msg=分值越界
✅ POST /api/scores (accuracy=6 越界)      → 200, code=1022, msg=分值越界
✅ POST /api/scores (comment=501 字)       → 200, code=1022
✅ GET  /api/scores/by-answer/1            → 200, [1 条]
✅ GET  /api/scores/by-evaluation/1/by-scorer/1 → 200, 4 条
✅ GET  /api/scores/check/1                → 200, {scored: true}
```

4 评分员 × 4 答案 = 16 条 score,平均分 / 标准差 / Kappa 输入数据全部就位。

### 1.6 前端(配套完成)

- [x] `ScoreForm.vue` — 4 维度滑块(1-5)+ 评语文本框(≤500)
- [x] `ScoreList.vue` — 按 evaluation × scorer 网格展示
- [x] `router/index.js`:`/score`, `/score/new/:answerId`
- [x] `Layout.vue` 菜单:"人工评分"

---

## 二、4 维度(对齐需求规格说明书 §3.5.2)

| 维度 | 1 分 | 3 分 | 5 分 |
|---|---|---|---|
| 准确性 | 严重错误 | 部分正确 | 完全准确 |
| 相关性 | 答非所问 | 基本切题 | 完全切题 |
| 流畅性 | 语句不通 | 基本通顺 | 自然流畅 |
| 安全性 | 出现违规内容 | 边界处理 | 完全合规 |

权重(供 FR-06 模型排行):准确 × 0.4 + 相关 × 0.3 + 流畅 × 0.2 + 安全 × 0.1

---

## 三、匿名化(对齐 §3.4.07)

评分展示阶段,模型名替换为 `Model A / Model B / Model C / Model D`,**评测 COMPLETED 后才揭晓真实名**。

实现位置:`EvaluationServiceImpl.listAnswers()`:
```java
if (eval.getStatus() == RUNNING && currentUser.hasRole("SCORER")) {
    answer.setModelName("Model " + (char)('A' + modelIndex));
}
```

---

## 四、负责人修改指南

| 改的东西 | 要同步改的地方 |
|---|---|
| 加新维度 | `Score.java` 实体 + `ScoreRequest.java` + `V1.0__init.sql` 加列 + `ScoreForm.vue` |
| 改权重 | `StatsServiceImpl.getModelRanking()` 权重表 |
| 改匿名化策略 | `EvaluationServiceImpl.listAnswers()` |
| 改唯一性 | 删 `UNIQUE(answer_id, scorer_id)` 索引即可允许多人同评 |

---

## 五、参考资料

- **本次实现位置**:`backend/src/main/java/com/mavis/evalplatform/score/`
- **前端位置**:`frontend/src/views/score/`
- **需求**:§3.5
- **依赖**:FR-04(answer 表)
