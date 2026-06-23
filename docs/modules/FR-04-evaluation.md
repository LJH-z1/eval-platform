# FR-04 多模型调用与对比展示 — ✅ 已实现

> **负责人:梁倩倩**(主)+ 宋子翔(并行调度协助)
> **关联分支**:`feature/eval`(同 FR-07 一起)
> **预计工作量**:7 人日
> **实际状态**:5 个适配器 + 异步评测 + MOCK 模式 全部跑通
> **对应评分层**:基础层 + 挑战层

---

## 一、任务完成清单

### 1.1 模型适配器(由向锏楠主写,实际落地 5 个 OpenAI 兼容实现 + MOCK 模式)

- [x] `M3Adapter` — MiniMax M3(OpenAI 兼容协议,WebClient HTTPS)
- [x] `ZhipuAdapter` — 智谱 GLM-4(OpenAI 兼容)
- [x] `QwenAdapter` — 通义千问(OpenAI 兼容)
- [x] `KimiAdapter` — 月之暗面(OpenAI 兼容)
- [x] `OpenAiAdapter` — 通用 OpenAI 兼容协议(覆盖 WENXIN/CUSTOM/第三方)
- [x] `ModelAdapterFactory` — Spring `@Component` 自动注入,按 `provider` 分发;5 个 provider 启动日志:
  ```
  [ModelAdapter] registered provider=M3
  [ModelAdapter] registered provider=ZHIPU
  [ModelAdapter] registered provider=QWEN
  [ModelAdapter] registered provider=KIMI
  [ModelAdapter] registered provider=OPENAI
  ```
- [x] **MOCK 模式**:`apiKey="MOCK"` 或 `endpoint=mock://*` 时短路返回假答案,不再真调远端
  - 触发位置:`OpenAiCompatibleAdapter.call()` 顶部
  - 用途:无真实 API Key 也能跑全链路 E2E

### 1.2 Evaluation Service(2 天)

- [x] `EvaluationServiceImpl.create` — 校验:模型数 ≥ 2(1 个抛 1022)、单次问题数 ≤ 50
- [x] `EvaluationServiceImpl.start(id)` — 启动 `EvaluationRunner`(同事务)
- [x] `EvaluationServiceImpl.getById` / `page` / `listAnswers`
- [x] `EvaluationServiceImpl.listAnswers` 匿名化:状态为 RUNNING 时 `modelName` 替换为 `Model A/B/C/D`

### 1.3 EvaluationRunner(核心,2 天)

- [x] `@Async("evaluationExecutor")` + `CompletableFuture` 并行调用
- [x] 线程池配置:`ThreadPoolConfig.evaluationExecutor`,core=8/max=16/queue=200
- [x] 单模型失败 catch → 写 `error_answer`(不抛、不影响其他)
- [x] 重试 1 次后仍失败则写 `errorCode / errorMessage / status=ERROR`
- [x] 全部完成 → `evaluation.status = COMPLETED`
- [x] 超时:WebClient 5s/30s(可配)

### 1.4 单元测试(MOCK 覆盖)

- [x] 双模型并行 → 2 条 answer,耗时 ≈ 单模型
- [x] 四模型并行 → 4 条 answer,耗时 ≈ 单模型
- [x] 单模型失败 catch → 1 ERROR + 3 OK
- [x] 匿名化:`getById(RUNNING)` 返 `Model A/B/C/D`;`COMPLETED` 返真实名
- [x] MOCK 模式:`apiKey=MOCK` 不真调远端,固定回 `"【MOCK-{provider}】 ..."`

### 1.5 前端(配套完成)

- [x] `EvaluationList.vue` — 表格 + 状态徽章 + 启动/查看按钮
- [x] `EvaluationNew.vue` — 多选模型 + 多选问题 + 实时预览
- [x] `EvaluationDetail.vue` — 答案并排展示,差异高亮,匿名化标签
- [x] `router/index.js`:`/evaluation`, `/evaluation/new`, `/evaluation/:id`
- [x] **Arena.vue**(LMArena 风格盲评页) — 调用 `listEnabledModels()` 拉真实模型池,逐对比较

---

## 二、关键代码参考(实际实现)

```java
@Async("evaluationExecutor")
public void run(Long evaluationId) {
    Evaluation eval = evaluationMapper.selectById(evaluationId);
    List<ModelConfig> models = modelService.listByIds(eval.getModelIds());
    List<Question> questions = questionService.listByIds(eval.getQuestionIds());

    for (Question q : questions) {
        List<CompletableFuture<Answer>> futures = models.stream()
            .map(m -> CompletableFuture.supplyAsync(
                () -> callWithRetry(m, q), evaluationExecutor)
                .exceptionally(ex -> buildErrorAnswer(m, q, ex)))
            .toList();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        futures.forEach(f -> answerMapper.insert(f.join()));
    }
    eval.setStatus(EvaluationStatus.COMPLETED);
    evaluationMapper.updateById(eval);
}
```

---

## 三、MOCK 模式触发规则

| 触发条件 | 优先级 |
|---|---|
| `apiKey` 字符串 equals `"MOCK"`(大小写敏感) | 高 |
| `endpoint` 以 `mock://` 开头 | 中 |
| 其他情况 | 走真实 HTTP 调用 |

返回值:`【MOCK-{provider}】 这是对"{question.content 前 30 字}"的模拟回答 (latency~{N}ms)`

---

## 四、端到端验证(MOCK 跑通)

```
✅ POST /api/evaluations (2 模型 × 4 题)   → 200, id=1
✅ POST /api/evaluations/1/run            → 200, 启动异步
   [EvaluationRunner] running eval=1 models=[17, 18] questions=4
   [M3Adapter] MOCK answer for Q1: "【MOCK-M3】 ..."
   [OPENAIAdapter] MOCK answer for Q1: "【MOCK-OPENAI】 ..."
   ... 共 4×2=8 条 answer 落库
✅ GET /api/evaluations/1/answers         → 200, 8 条
✅ GET /api/evaluations/1 (status)        → 200, status=COMPLETED
```

---

## 五、负责人修改指南

| 改的东西 | 要同步改的地方 |
|---|---|
| 加新 provider | `ModelAdapter` 子类 + `provider` 枚举白名单 + `ModelAdapterFactory` 自动注册 |
| 改并行策略 | `ThreadPoolConfig.evaluationExecutor` |
| 加 SSE 流式 | `EvaluationController.stream` + `SseEmitterRegistry` + `Adapter.stream()`(本期未实现,留作 v2) |
| 改 MOCK 文案 | `OpenAiCompatibleAdapter.call()` MOCK 分支 |

---

## 六、参考资料

- **本次实现位置**:`backend/src/main/java/com/mavis/evalplatform/evaluation/{adapter,service,entity}/`
- **前端位置**:`frontend/src/views/evaluation/`, `frontend/src/views/arena/`
- **架构**:`§4.2.4`
