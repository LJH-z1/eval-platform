# 多模型回答对比与评测平台 — 系统架构

> 课程项目第 7 项,软件测试小组
> 本文档是 **架构骨架版**,所有具体业务实现由各模块负责人按 `CONTRIBUTING.md` 完成

---

## 一、系统总览

### 1.1 一句话目标

**统一输入问题,并行调用多个国产大模型 API,人工多维度评分与一致性分析,为 LLM 选型提供工程化评测方案。**

### 1.2 角色与分工(对齐需求规格说明书 §1.5.2)

| 成员 | 角色 | 负责模块 | 评分层 |
|---|---|---|---|
| **刘家豪**(组长) | 后端主程 | FR-01 用户登录与权限 / common / config / 整体协调 | 基础层 + 提高层 |
| 向锏楠 | 后端开发 | FR-02 模型配置 / FR-03 问题输入 | 基础层 + 提高层 |
| 梁倩倩 | 后端开发 | FR-04 多模型调用 / FR-07 成本统计 | 基础层 + 挑战层 |
| 靳磊 | 前端开发 | 全部 Vue 页面 / Pinia / 路由 / UI | 贯穿 |
| 宋子翔 | 算法 / 数据 | FR-05 多维评分 / FR-06 Fleiss Kappa 一致性 | 提高层 + 挑战层 |
| 周文泽 | 测试 / DevOps | FR-08 报告导出 / 自动化测试 / CI / 答辩 | 基础层 + 提高层 |

### 1.3 三大业务主流程

```
┌──────────────────────────────────────────────────────────────────┐
│  流程 A:评测创建与执行                                            │
│  评测组织者(org1)→ 创建评测(选模型、选问题) → 启动 → 系统并行调用  │
│  4 个模型 → 答案入库(answer 表) → 流式回显前端                      │
└──────────────────────────────────────────────────────────────────┘
┌──────────────────────────────────────────────────────────────────┐
│  流程 B:人工盲评                                                  │
│  评分员(scorer1/2/3)→ 进入评分界面 → 模型名匿名(Model A/B/C)     │
│  → 4 维度 1-5 分打分 → 提交 → 写 score 表(每个回答 × 评分员唯一)   │
└──────────────────────────────────────────────────────────────────┘
┌──────────────────────────────────────────────────────────────────┐
│  流程 C:一致性分析与导出                                          │
│  评测组织者 → 触发 Fleiss Kappa 计算(可缓存 5min)                 │
│  → 展示一致性等级 + 争议项 + 评分员排行 + 模型排名                   │
│  → 一键导出 Excel/PDF 报告                                         │
└──────────────────────────────────────────────────────────────────┘
```

---

## 二、技术栈

### 2.1 后端

| 类别 | 技术 | 版本 | 选型理由 |
|---|---|---|---|
| 基础框架 | Spring Boot | 3.2.x | 生态完善、自动配置 |
| 安全 | Spring Security + jjwt | 6.x / 0.12.x | 无状态 JWT 鉴权 |
| ORM | MyBatis-Plus | 3.5.x | 动态 SQL + 分页插件 |
| 数据库 | MySQL | 8.0+ | 团队熟悉 |
| 缓存 | Redis | 7.x | 限流计数 + Kappa 缓存 |
| API 文档 | SpringDoc OpenAPI | 2.3.x | Swagger 替代品 |
| 工具 | Hutool | 5.8.x | 国产工具库 |
| 导出 | EasyExcel + iText | 3.3 / 8 | 报告生成 |
| 测试 | JUnit 5 + Mockito | - | 标准测试框架 |

### 2.2 前端

| 类别 | 技术 | 版本 |
|---|---|---|
| 框架 | Vue 3 + Vite | 3.4 / 5+ |
| UI | Element Plus | 2.7+ |
| 状态 | Pinia | 2.x |
| 路由 | Vue Router | 4.x |
| HTTP | Axios | 1.x |
| 图表 | ECharts | 5.x |
| 差异 | diff | 5.x |
| 渲染 | marked | 12.x |

---

## 三、目录结构

```
eval-platform/
├── backend/                              # Spring Boot 后端
│   ├── pom.xml
│   ├── src/main/java/com/mavis/evalplatform/
│   │   ├── EvalPlatformApplication.java
│   │   ├── common/                       # 【刘家豪】基础设施(所有模块用)
│   │   │   ├── result/    Result / PageResult 统一响应
│   │   │   ├── exception/ BusinessException / ErrorCode / GlobalExceptionHandler
│   │   │   ├── util/      AesUtil / WebUtil
│   │   │   ├── annotation/RateLimit
│   │   │   └── aspect/    RateLimitAspect
│   │   ├── config/                       # 【刘家豪】通用配置
│   │   │   ├── SecurityConfig / CorsProperties
│   │   │   ├── MyBatisPlusConfig / RedisConfig
│   │   │   ├── OpenApiConfig / ExecutorConfig
│   │   ├── auth/                         # 【刘家豪】FR-01 用户登录与权限
│   │   │   ├── controller/   AuthController / UserController
│   │   │   ├── service/      AuthService / UserService / JwtService / UserDetailsServiceImpl / AuditLogService
│   │   │   ├── entity/       User / AuditLog / Role
│   │   │   ├── dto/          LoginRequest / LoginResponse / RegisterRequest / UserInfo / ChangePasswordRequest
│   │   │   ├── mapper/       UserMapper / AuditLogMapper
│   │   │   ├── filter/       JwtAuthenticationFilter (含 AuthenticatedUser record)
│   │   ├── model/                        # 【向锏楠】FR-02 模型配置管理
│   │   ├── question/                     # 【向锏楠】FR-03 问题输入与管理
│   │   ├── adapter/                      # 【向锏楠+宋子翔】模型适配器
│   │   │   ├── ModelAdapter (统一抽象接口)
│   │   │   ├── M3Adapter / ZhipuAdapter / QwenAdapter / WenxinAdapter / KimiAdapter
│   │   │   └── ModelAdapterFactory
│   │   ├── evaluation/                   # 【梁倩倩】FR-04 多模型调用与对比展示
│   │   │   ├── EvaluationRunner (并行调度)
│   │   │   ├── SSE 流式输出
│   │   ├── score/                        # 【宋子翔】FR-05 多维评分
│   │   ├── stats/                        # 【宋子翔】FR-06 一致性分析 + Fleiss Kappa
│   │   ├── billing/                      # 【梁倩倩】FR-07 成本与耗时统计
│   │   ├── export/                       # 【周文泽】FR-08 报告导出
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/   V*.sql 数据库表结构
│   └── src/test/...                       # 各模块单元测试
│
├── frontend/                             # Vue 3 前端
│   ├── package.json
│   ├── vite.config.js
│   ├── index.html
│   └── src/
│       ├── api/                          # 后端各模块负责人提供接口定义
│       │   ├── auth.js        【刘家豪】
│       │   ├── model.js       【向锏楠】
│       │   ├── question.js    【向锏楠】
│       │   ├── evaluation.js  【梁倩倩】
│       │   ├── score.js       【宋子翔】
│       │   ├── stats.js       【宋子翔】
│       │   ├── billing.js     【梁倩倩】
│       │   └── export.js      【周文泽】
│       ├── views/
│       │   ├── auth/         【刘家豪】Login/Register/Profile/UserManagement
│       │   ├── model/        【向锏楠】
│       │   ├── question/     【向锏楠】
│       │   ├── evaluation/   【梁倩倩】
│       │   ├── score/        【宋子翔】
│       │   ├── stats/        【宋子翔】
│       │   ├── billing/      【梁倩倩】
│       │   └── export/       【周文泽】
│       ├── components/      【靳磊】公共组件
│       ├── stores/          【靳磊】Pinia stores
│       ├── router/          【靳磊】Vue Router + 全局守卫
│       ├── utils/           【靳磊】request.js (Axios 拦截器)
│       ├── styles/          【靳磊】global.css
│       ├── App.vue / main.js
│
├── deploy/                               # 部署
│   ├── start.sh / start.bat
│   ├── nginx.conf
│
├── docs/
│   ├── 01-需求规格说明书.docx
│   ├── 02-架构设计说明书.docx
│   ├── 03-测试计划说明书.docx
│   ├── ARCHITECTURE.md                   # 本文件
│   ├── CONTRIBUTING.md                   # 【必读】开发者协作指南
│   └── modules/                          # 各模块负责人的 TODO
│       ├── FR-01-auth.md                 # 【刘家豪】✅ 已实现
│       ├── FR-02-model.md                # 【向锏楠】TODO
│       ├── FR-03-question.md             # 【向锏楠】TODO
│       ├── FR-04-evaluation.md           # 【梁倩倩】TODO
│       ├── FR-05-score.md                # 【宋子翔】TODO
│       ├── FR-06-stats.md                # 【宋子翔】TODO
│       ├── FR-07-billing.md              # 【梁倩倩】TODO
│       └── FR-08-export.md               # 【周文泽】TODO
│
├── .github/
│   ├── ISSUE_TEMPLATE/
│   │   ├── bug_report.md
│   │   └── feature_request.md
│   └── PULL_REQUEST_TEMPLATE.md
│
├── .gitignore
├── README.md                             # 入门 + 一键启动
└── pom.xml (Maven wrapper 可选)
```

---

## 四、模块依赖关系(后端)

```
                           ┌──────────────┐
                           │     auth     │ ← 刘家豪
                           │  (登录鉴权)   │   提供:SecurityContext/JwtService/AuthenticatedUser
                           └──────┬───────┘
                                  │
                                  ▼  提供 JWT 解析、用户上下文
        ┌──────────┐  ┌─────────┐  ┌──────────┐  ┌──────────┐
        │  model   │←─┤ question│  │evaluation│  │  score   │
        │ (模型配置)│  │ (问题)  │←→│  (评测)  │←─│  (评分)  │
        └─────┬────┘  └─────────┘  └─────┬────┘  └────┬─────┘
              │                          │             │
              │                          ▼             ▼
              │                    ┌──────────┐   ┌──────────┐
              └───────────────────→│  adapter │   │  stats   │
                                   │ (LLM API)│   │(一致性)  │
                                   └──────────┘   └──────────┘
                                                      │
                                                      ▼
                                   ┌──────────┐  ┌──────────────────┐
                                   │ billing  │  │     export       │
                                   │ (成本)   │  │  (Excel/PDF)     │
                                   └──────────┘  └──────────────────┘
```

**关键依赖原则**
- `auth` 不依赖任何业务模块(基础设施,必须最先就绪)
- `model` 被 `evaluation`、`adapter` 引用
- `question` 被 `evaluation` 引用
- `evaluation` 是核心调度中枢,引用 `model`、`question`、`adapter`
- `score` 引用 `evaluation`(为 answer 评分)
- `stats` 引用 `score`(算 Kappa)
- `billing` 引用 `evaluation`(统计 answer 表的 token/latency)
- `export` 引用 `evaluation`、`score`、`stats`(汇总导出)
- 所有模块都依赖 `auth` 的 JwtAuthenticationFilter(由 Spring Security 全局拦截)

---

## 五、数据库设计(对齐架构设计说明书 §5)

### 5.1 E-R 图

```
┌────────┐         ┌────────────┐         ┌────────┐
│  User  │────┬───→│Evaluation  │←────┬──│ Model  │
└────────┘    │    └────────────┘     │   └────────┘
              │           │            │
              │           ▼            │
              │     ┌──────────┐       │
              │     │ Question │←──────┘
              │     └──────────┘
              │           │
              │           ▼
              │     ┌──────────┐
              │     │  Answer  │────────────────┐
              │     └──────────┘                │
              │           │                     │
              │           ▼                     │
              │     ┌──────────┐                │
              └────→│  Score   │                │
                    └──────────┘                │
                                                │
              ┌──────────────────────────────┘
              │
              ▼ (Model 是 Answer 的多对一端)
```

### 5.2 表结构(详见 `backend/src/main/resources/db/migration/V1.0__init.sql`)

| 表 | 关键字段 | 负责人 |
|---|---|---|
| `user` | id, username, password(BCrypt), email, role, status, failed_count, locked_until | 刘家豪 |
| `model_config` | id, name, provider, api_key(AES-256 加密), endpoint, model_version, temperature, top_p, max_tokens, price_per_k, status | 向锏楠 |
| `question` | id, content, category, difficulty, type, expected_answer, created_by, deleted | 向锏楠 |
| `evaluation` | id, name, description, created_by, status, model_ids, question_ids, started_at, finished_at | 梁倩倩 |
| `answer` | id, evaluation_id, question_id, model_id, content, latency_ms, token_input, token_output, estimated_cost, error_code, error_message | 梁倩倩 |
| `score` | id, answer_id, scorer_id, accuracy(1-5), relevance(1-5), fluency(1-5), safety(1-5), comment | 宋子翔 |
| `audit_log` | id, user_id, username, action, target, ip, status, detail | 刘家豪 |

### 5.3 通用约定

- 主键:自增 BIGINT
- 软删除:`deleted` 字段(TINYINT, 0/1),MyBatis-Plus 逻辑删除插件自动处理
- 时间:LocalDateTime,Java 端 `LocalDateTime.now()`,DB 默认 `CURRENT_TIMESTAMP`
- 索引:外键字段全部建索引(虽然不建外键约束)

---

## 六、统一接口契约

### 6.1 统一响应格式

```json
{
  "code": 200,
  "message": "ok",
  "data": { ... },
  "timestamp": 1718600000000
}
```

- `code = 200` 成功,前端 `request.js` 拦截器自动解包 data
- 业务错误码 1001-1099(详见 `common/exception/ErrorCode.java`)
- HTTP 状态码与业务码独立:401 未登录、403 无权限、429 限流、500 服务器错误

### 6.2 鉴权约定

- 登录后返回 JWT,前端存 `localStorage`
- 后续请求 Header:`Authorization: Bearer <token>`
- `JwtAuthenticationFilter` 自动解析并写 `SecurityContextHolder`
- Controller 拿用户上下文: `@AuthenticationPrincipal AuthenticatedUser user`
- RBAC 角色:ADMIN / ORGANIZER / SCORER / VISITOR
- 方法级权限:`@PreAuthorize("hasRole('XXX')")`

### 6.3 异常处理约定

业务层抛 `BusinessException(ErrorCode.XXX)` 或 `BusinessException(msg)`,由 `GlobalExceptionHandler` 统一包装为 `Result.error(code, message)` 返回。前端 `request.js` 拦截器统一弹 `ElMessage.error(message)`。

### 6.4 接口前缀

- `/api/auth/*` 认证(刘家豪)
- `/api/users/*` 用户管理(刘家豪)
- `/api/models/*` 模型配置(向锏楠)
- `/api/questions/*` 问题管理(向锏楠)
- `/api/evaluations/*` 评测(梁倩倩)
- `/api/scores/*` 评分(宋子翔)
- `/api/stats/*` 统计(宋子翔)
- `/api/billing/*` 成本(梁倩倩)
- `/api/export/*` 导出(周文泽)

---

## 七、关键技术点

### 7.1 适配器模式(由向锏楠 + 宋子翔实现)

```java
public interface ModelAdapter {
    String provider();
    ModelResponse call(ModelRequest request);
    Flux<String> stream(ModelRequest request);
    boolean healthCheck();
    BigDecimal pricePerKToken();
}
```

5 个实现:M3Adapter / ZhipuAdapter / QwenAdapter / WenxinAdapter / KimiAdapter

### 7.2 并行调用(由梁倩倩实现)

```java
@Async("evaluationExecutor")
public CompletableFuture<List<Answer>> runQuestion(...) {
    return CompletableFuture.allOf(
        models.stream().map(m -> CompletableFuture.supplyAsync(
            () -> callModelWithRetry(m, q), evaluationExecutor))
        .toArray(new CompletableFuture[0]))
    .thenApply(...);
}
```

### 7.3 SSE 流式输出(由梁倩倩实现)

```
GET /api/evaluations/{id}/stream
data: {"questionId":1,"modelId":1,"chunk":"你好","done":false}
data: {"questionId":1,"modelId":1,"chunk":"！","done":true}
```

### 7.4 Fleiss Kappa 算法(由宋子翔实现,见 FR-06 详细文档)

```java
kappa = (P̄ - P̄e) / (1 - P̄e)
```

### 7.5 差异高亮(由靳磊实现)

前端用 `diff` 库,后端不参与。

### 7.6 限流(由刘家豪已实现)

```java
@RateLimit(limitPerMinute = 30, key = "auth.login")
public Result<LoginResponse> login(...) { ... }
```

---

## 八、安全设计(对齐架构设计说明书 §8)

| 威胁 | 措施 | 实现位置 |
|---|---|---|
| SQL 注入 | MyBatis 参数化 | 所有 Mapper |
| XSS | Vue 默认转义 + 后端输入过滤 | 前端 + 后端 |
| CSRF | JWT(无状态天然防) | JwtAuthenticationFilter |
| 越权 | RBAC + `@PreAuthorize` | SecurityConfig + 各 Controller |
| 暴力破解 | 登录失败计数 + 锁定 30min | AuthService |
| API 滥用 | Redis 限流(60次/min) | RateLimitAspect |
| 敏感信息泄露 | API Key AES-256 加密、密码不返回、日志脱敏 | AesUtil |

---

## 九、部署

详见 `deploy/` 目录与根 `README.md` 的「本地启动」章节。

---

## 十、上手步骤

1. **看本文档** → 了解整体架构
2. **看 `CONTRIBUTING.md`** → 了解协作规范、分支策略、提交流程
3. **看 `docs/modules/FR-XX-xxx.md`** → 你的具体任务的清单
4. **在 `feature/xxx` 分支开发** → 不要直接推 main
5. **本地 `mvn test` + `npm run build` 通过** → 提 PR 到 `develop`
6. **每日 21:00 站会** → 同步进度、阻塞问题升级

---

## 十一、修订历史

| 版本 | 日期 | 修订人 | 说明 |
|---|---|---|---|
| V0.1 | 2026-06-12 | 刘家豪 | 初始草稿 |
| V0.5 | 2026-06-16 | 向锏楠 | 数据库与接口补充 |
| V1.0 | 2026-06-17 | 梁倩倩 | 正式发布版 |
