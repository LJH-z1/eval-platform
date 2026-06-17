# 单元测试说明 — 架构骨架版

> 本目录下的具体业务测试(AuthServiceTest / JwtServiceTest / UserServiceTest)暂未启用,
> 由各模块负责人在自己分支上实现,覆盖对齐测试计划文档 §6 中的 TC-XX-XXX 编号。

## 现有测试

- `SkeletonSmokeTest` — 骨架冒烟,验证关键基础类加载,不依赖业务实现

## 各模块需补的测试

| 模块 | 必测用例(对齐测试计划) | 建议文件名 |
|---|---|---|
| FR-01 auth | TC-01-001 ~ 006 | AuthServiceTest.java |
| FR-02 model | TC-02-001 ~ 006 | ModelServiceTest.java |
| FR-03 question | TC-03-001 ~ 005 | QuestionServiceTest.java |
| FR-04 evaluation | TC-04-001 ~ 008 | EvaluationServiceTest.java |
| FR-05 score | TC-05-001 ~ 006 | ScoreServiceTest.java |
| FR-06 stats | TC-06-001 ~ 006 + TC-KAPPA-001 ~ 004 | FleissKappaServiceTest.java |
| FR-07 billing | TC-07-001 ~ 005 | BillingServiceTest.java |
| FR-08 export | TC-08-001 ~ 006 | ReportExportServiceTest.java |

覆盖率要求:Service 层 ≥ 60%(对齐测试计划 §9.2 准出标准)
