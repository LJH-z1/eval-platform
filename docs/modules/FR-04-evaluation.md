# FR-04 多模型调用与对比展示 — TODO

> **负责人:梁倩倩**(主)+ 宋子翔(并行调度协助)
> **关联分支**:`feature/eval`(同 FR-07 一起)
> **预计工作量**:7 人日
> **对应评分层**:基础层 + 挑战层

## 一、任务清单

### 1.1 模型适配器(由向锏楠主写,梁倩倩协助 — 已分到 FR-04,见下方"1.7")(3 天)

- [ ] `M3Adapter` — MiniMax M3(OpenAI 兼容协议,使用 webClient 调用 HTTPS)
- [ ] `ZhipuAdapter` — 智谱 GLM-4(自有协议)
- [ ] `QwenAdapter` — 通义千问(OpenAI 兼容)
- [ ] `WenxinAdapter` — 文心一言(Access Token 鉴权)
- [ ] `KimiAdapter` — 月之暗面(OpenAI 兼容)
- [ ] `ModelAdapterFactory` — Spring 收集所有 `@Component ModelAdapter`,按 provider 分发

### 1.2 Evaluation Service(2 天)

- [ ] `EvaluationService.create` — 校验:至少 2 个模型、单次问题数 ≤ 50
- [ ] `EvaluationService.start` — 异步启动 EvaluationRunner
- [ ] `EvaluationService.getById` / `page` / `listAnswers`

### 1.3 EvaluationRunner(核心,2 天)

- [ ] `@Async("evaluationExecutor")` + `CompletableFuture` 并行调用
- [ ] 单模型失败 catch → 写 error_answer,不影响其他模型
- [ ] 重试 1 次后仍失败则记 errorCode / errorMessage
- [ ] 全部完成 → status = COMPLETED
- [ ] 超时控制:总耗时 ≤ 10 分钟

### 1.4 SSE 流式输出(1 天)

- [ ] `EvaluationController.stream` 用 `Flux<ServerSentEvent<String>>`
- [ ] `SseEmitterRegistry` 管理 emitter 生命周期
- [ ] 适配器 stream() 推送 chunk,前端逐字显示
- [ ] Nginx 反代需要 `proxy_buffering off`(已在 deploy/nginx.conf 配置)

### 1.5 单元测试(1 天) — 覆盖 TC-04-001 ~ 008

- [ ] 双模型并行
- [ ] 四模型并行(总耗时 ≈ 单模型)
- [ ] 流式输出(用 step verifier)
- [ ] 单模型失败不影响其他
- [ ] 差异高亮(后端不做,前端用 diff 库)
- [ ] 匿名化(评分展示阶段模型名替换为 Model A/B)
- [ ] 单模型超时
- [ ] 评测时间统计

## 二、关键代码参考(架构设计说明书 §4.2.4)

```java
@Async("evaluationExecutor")
public CompletableFuture<List<Answer>> runQuestion(...) {
    List<CompletableFuture<Answer>> futures = models.stream()
        .map(model -> CompletableFuture.supplyAsync(
            () -> callModelWithRetry(model, question), evaluationExecutor)
            .exceptionally(ex -> buildErrorAnswer(model, ex)))
        .toList();
    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .thenApply(v -> futures.stream().map(CompletableFuture::join).toList());
}
```

## 三、SSE 推送格式(对齐需求规格说明书 §3.4.03)

```
data: {"questionId":1,"modelId":1,"chunk":"你好","done":false}
data: {"questionId":1,"modelId":1,"chunk":"！","done":true}
```

## 四、参考资料

- 完整参考:`eval-platform-reference/backend/src/main/java/com/mavis/evalplatform/{adapter,evaluation}/`
- 架构设计:§4.2.4 / §7.1 / §7.2 / §7.3

## 五、给前端靳磊的接口

- 启动评测:POST /api/evaluations/{id}/run
- SSE 订阅:GET /api/evaluations/{id}/stream
- 拉答案:GET /api/evaluations/{id}/answers
