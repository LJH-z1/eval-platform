# 开发者协作指南

> 面向本仓库所有贡献者(尤其是第一次参与的小组成员)
> 阅读对象:刘家豪(组长/FR-01)、向锏楠(FR-02/03)、梁倩倩(FR-04/07)、靳磊(前端)、宋子翔(FR-05/06)、周文泽(FR-08/测试)

---

## 一、阅读顺序

1. 根目录 `README.md` — 启动 + 整体介绍
2. `docs/ARCHITECTURE.md` — **必读**架构总览
3. `docs/modules/FR-XX-xxx.md` — **你的任务清单**(本文件)
4. `01-需求规格说明书.docx` / `02-架构设计说明书.docx` / `03-测试计划说明书.docx` — 原始需求
5. 找刘家豪要 token 拉 GitHub 仓库、加入飞书任务表

---

## 二、分支策略

| 分支 | 负责人 | 说明 |
|---|---|---|
| `main` | 刘家豪(only) | 受保护,只有组长能合并,对应"可演示"版本 |
| `develop` | 全员 | 集成分支,各 feature 合到这里 |
| `feature/auth` | 刘家豪 | FR-01(已完成) |
| `feature/model` | 向锏楠 | FR-02 + FR-03 |
| `feature/eval` | 梁倩倩 | FR-04 + FR-07 |
| `feature/frontend` | 靳磊 | 前端整体 |
| `feature/score` | 宋子翔 | FR-05 + FR-06 |
| `feature/test` | 周文泽 | FR-08 + 测试 + CI |

**流程**:`feature → develop → main`(每日合并到 develop,每周发布到 main)

---

## 三、提交规范

### 3.1 Commit 消息格式

```
<type>(<scope>): <subject>

<body>

<footer>
```

- type: `feat` / `fix` / `refactor` / `test` / `docs` / `chore` / `style`
- scope: 模块名,如 `auth` / `model` / `eval` / `frontend` / `score`
- subject: 50 字以内,中文,祈使句

示例:
```
feat(auth): 实现用户登录、Token 签发与连续失败锁定
fix(model): 修复 API Key 解密空指针
docs(sad): 补充 §4.2.4 业务流程图
test(score): 补充 Fleiss Kappa 教科书示例测试
```

### 3.2 PR 流程

1. **本地自测**:`mvn test` + `npm run build` 通过
2. **同步 develop**:`git pull origin develop` 后 rebase
3. **提 PR**:`feature/xxx → develop`,标题同 commit 格式
4. **填 PR 模板**:在 `.github/PULL_REQUEST_TEMPLATE.md` 中勾选
5. **请 1 人 Code Review**:组内任意成员 + 组长(刘家豪)最终合并
6. **合并后删分支**

### 3.3 禁止事项

- ❌ 不要把 `target/`、`node_modules/`、`*.log`、`.env` 提交
- ❌ 不要直接 push `main`
- ❌ 不要在一个 PR 里改跨模块的内容
- ❌ 不要把 `JWT_SECRET` / `ENCRYPTION_KEY` / 数据库密码等敏感信息硬编码

---

## 四、代码规范

### 4.1 后端 (Java / Spring Boot)

- **命名**:类名 UpperCamelCase,方法/变量 lowerCamelCase,常量 UPPER_SNAKE
- **包结构**:每个模块固定 `controller / service / entity / dto / mapper`,根据需要加 `config / filter / aspect`
- **Lombok**:用 `@Data`(或 `@Getter`+`@Setter`)、`@Builder`、`@Slf4j`,不要写样板 getter/setter
- **注释**:public 方法 ≥ 80% 注释率(对齐 §4.4 可维护性)
- **日志**:SLF4J + Lombok `@Slf4j`,分级记录;**禁止** `System.out.println`
- **异常**:业务层抛 `BusinessException(ErrorCode.XXX)`,**禁止**直接抛 RuntimeException
- **响应**:Controller 方法返回 `Result<T>`,**禁止**直接返回实体类
- **依赖**:Spring Security 拿用户用 `@AuthenticationPrincipal AuthenticatedUser user`,**禁止**直接读 `SecurityContextHolder`

### 4.2 前端 (Vue 3 / TypeScript 可选)

- **组件**:Composition API + `<script setup>`
- **命名**:组件 PascalCase,变量 camelCase,常量 UPPER_SNAKE
- **状态管理**:Pinia + `useStore`,**禁止**直接修改 state(用 action)
- **路由**:动态路由懒加载,守卫统一在 `router/index.js`
- **HTTP**:`utils/request.js` 已封装,**禁止**直接 axios.get
- **样式**:scoped CSS,**禁止**全局污染

### 4.3 注释与文档

- 公共方法必须 JavaDoc / JSDoc
- 模块入口类有顶层注释说明职责
- 复杂业务逻辑写明"为什么"(what 看代码就懂,why 写注释)

---

## 五、测试规范

### 5.1 单元测试

- **位置**:`src/test/java/com/mavis/evalplatform/<module>/`
- **覆盖率目标**:Service 层 ≥ 60%(对齐测试计划 §9.2 准出标准)
- **命名**:`XxxServiceTest` / `XxxServiceUnitTest`
- **断言**:JUnit 5 + AssertJ / Hamcrest,**禁止**只用 `assertTrue(x == y)`
- **Mock**:Mockito `@Mock` + `@InjectMocks`,**禁止**打桩静态方法
- **覆盖规则**:正常路径 + 至少 1 个异常路径

### 5.2 接口测试

- 由周文泽负责(FR-08 模块),但**每个模块负责人都要写自己模块的 happy path 集成测试**
- 工具:REST Assured 或 Postman
- 位置:`src/test/java/com/mavis/evalplatform/<module>/integration/`

### 5.3 必测场景(从测试计划 §6 抽)

| 你的模块 | 必测场景 |
|---|---|
| FR-01 auth | TC-01-001 ~ 006 |
| FR-02 model | TC-02-001 ~ 006 |
| FR-03 question | TC-03-001 ~ 005 |
| FR-04 evaluation | TC-04-001 ~ 008 |
| FR-05 score | TC-05-001 ~ 006 |
| FR-06 stats | TC-06-001 ~ 006,TC-KAPPA-001 ~ 004 |
| FR-07 billing | TC-07-001 ~ 005 |
| FR-08 export | TC-08-001 ~ 006 |

---

## 六、每日站会

- **时间**:每晚 21:00(腾讯会议)
- **时长**:≤ 15 分钟
- **内容**:
  1. 昨天完成了什么
  2. 今天计划做什么
  3. 有什么阻塞需要升级

---

## 七、遇到问题怎么办

| 问题 | 找谁 |
|---|---|
| 接口契约不清楚 | 看 ARCHITECTURE.md §6,不确定问刘家豪 |
| 编译/依赖错误 | 刘家豪(后端) / 靳磊(前端) |
| 数据库表结构 | 看 `backend/src/main/resources/db/migration/V*.sql` |
| 测试不通过 | 自己 debug → 找相关模块负责人 → 找刘家豪 |
| 代码冲突 | 联系相关同事当面 merge,实在不行刘家豪出面 |
| 紧急 bug | 直接 @ 刘家豪(组长) |

---

## 八、对外协作

- 飞书任务表:每日更新
- 课程评审:每周三 14:00 答疑
- GitHub Issues:bug / feature 统一管理

---

## 九、验收标准(对齐测试计划 §9.2)

- ✅ 必测用例通过率 100%
- ✅ P0 缺陷 0 个
- ✅ P1 缺陷 0 个
- ✅ 单元测试覆盖率 ≥ 60%
- ✅ API P95 ≤ 500ms
- ✅ Fleiss Kappa 实测误差 < 0.001
- ✅ 演示主流程 5 分钟内可完成
- ✅ 报告导出 Excel + PDF 完整

---

## 十、附:刘家豪(组长)对每个人的期望

| 成员 | 期望 |
|---|---|
| 向锏楠 | 6-18 完成模型配置 + 问题管理 Service 层;6-19 单测覆盖;6-20 起配合梁倩倩联调 |
| 梁倩倩 | 6-18 完成 adapter 抽象 + 1 个 M3 适配器;6-19 完成并行调度;6-20 流式输出 |
| 靳磊 | 6-18 起跟各后端并行开发前端,每天截图发飞书;6-20 起联调 |
| 宋子翔 | 6-18 完成 ScoreService + Fleiss Kappa;6-19 准备教科书测试数据;6-20 起联调 |
| 周文泽 | 6-18 完成 Excel 导出 + 1 个 Service 单测;6-19 起 CI 跑通;6-20 整理缺陷表 |

---

> 各位看完后有问题直接群里 @ 我(刘家豪),我 24h 内一定回
