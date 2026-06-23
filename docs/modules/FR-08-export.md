# FR-08 报告导出与自动化测试 — ✅ 已实现(简化版)

> **负责人:周文泽**(测试负责人)
> **关联分支**:`feature/test`
> **预计工作量**:8 人日
> **实际状态**:CSV + HTML 双格式导出 + 元信息接口 全部跑通
> **对应评分层**:基础层 + 提高层

> ⚠️ **简化说明**:本期为减少外部依赖(EasyExcel 5MB / iText 8 6MB),改用 JDK 自带 + 手写 HTML 模板实现 CSV + HTML 双格式,Excel/Word 都能直接打开。完整 EasyExcel + iText 6 Sheet 版留作 v2。

---

## 一、任务完成清单

### 1.1 CSV 导出(1 天)— 简化方案

- [x] `ReportExportServiceImpl.exportCsv(evalId)` — `StringBuilder` 手写 CSV,无外部依赖
- [x] 列:`id, question_content, model_name, score_accuracy, score_relevance, score_fluency, score_safety, score_comment, token_input, token_output, latency_ms, estimated_cost, error_code`
- [x] UTF-8 BOM,Excel/WPS 双击不乱码
- [x] 端点:`GET /api/export/csv/{evalId}` → `text/csv` + `Content-Disposition: attachment; filename=eval_{id}_{YYYYMMDD}.csv`

### 1.2 HTML 导出(1.5 天)— 简化方案

- [x] `ReportExportServiceImpl.exportHtml(evalId)` — 手写 HTML 模板(单文件,内联 CSS)
- [x] 6 个章节(对齐原 §4.2.8 设计):
  1. 封面(评测名/创建者/创建时间/模型数/问题数)
  2. 评测概述
  3. 模型调用统计
  4. 评分统计(各模型各维度平均分)
  5. 一致性分析(Kappa 值,引用 FR-06)
  6. 详细结果表
- [x] 端点:`GET /api/export/html/{evalId}` → `text/html` + 附件头
- [x] Chrome 打开效果:4KB 单页白底,带标题/表格/章节分隔

### 1.3 元信息接口(0.5 天)

- [x] `GET /api/export/meta/{evalId}` — 返回 `{evalName, modelCount, questionCount, scoreCount, hasKappa, formats: ["csv","html"]}`

### 1.4 端到端验证

```
✅ GET /api/export/meta/1            → 200, {evalName:"Test Eval", modelCount:2, questionCount:4, scoreCount:16, hasKappa:true, formats:["csv","html"]}
✅ GET /api/export/csv/1             → 200, text/csv, body=1103 字节(UTF-8 BOM + 表头 + 16 行数据)
✅ GET /api/export/html/1            → 200, text/html, body=4211 字节(完整 6 章节)
```

### 1.5 前端(配套完成)

- [x] `ExportMain.vue` — 选择评测 + 元信息展示 + 2 个下载按钮(CSV/HTML)
- [x] `router/index.js`:`/export`
- [x] `Layout.vue` 菜单:"报告导出"

### 1.6 自动化测试体系(本期范围)

- [x] **E2E 脚本**:`D:\LJH\Project\test_score2.js`、`test_mysql.js` — 覆盖 FR-01/02/04/05/06/07/08 全链路
- [x] **MOCK 模式跑通**:无真实 API Key 也能完成全 8 模块 E2E
- [ ] ⚠️ **JUnit 单元测试**:Service 层覆盖率未达 60%(本期跳过,后续 v2 补)
- [ ] ⚠️ **JaCoCo 覆盖率报告**:未配置
- [ ] ⚠️ **GitHub Actions CI**:`.github/workflows/ci.yml` 未创建(本地开发模式,后续 v2 上 GitHub 时再补)

### 1.7 缺陷管理

- [x] 项目内问题用 TodoWrite 跟踪(本期 8 个模块全 ✅)
- [ ] ⚠️ GitHub Issues 模板:未配置(无 GitHub 仓)
- [ ] ⚠️ 飞书任务表:未对接

---

## 二、报告文件名格式

```
eval_{评测ID}_{YYYYMMDD}.csv
eval_{评测ID}_{YYYYMMDD}.html
```

例:`eval_1_20260621.csv`(打开是 UTF-8 CSV,16 行评分明细)
例:`eval_1_20260621.html`(打开是单页白底 HTML 报告)

---

## 三、测试覆盖率要求(对齐测试计划 §9.2)

| 项 | 目标 | 实际 |
|---|---|---|
| Service 层单测覆盖率 | ≥ 60% | ⚠️ 本期跳过(改用 E2E 脚本覆盖关键链路) |
| 关键接口(登录/调用/评分/导出) | 100% 覆盖 | ✅ E2E 全覆盖 |
| 必测用例 | 100% 通过 | ✅ 全部通过 |
| 端到端流程 | 通 | ✅ MOCK 模式跑通 |

---

## 四、负责人修改指南

| 改的东西 | 要同步改的地方 |
|---|---|
| 加 Sheet | `ReportExportServiceImpl.exportExcel()`(v2 启用)+ `XSSFWorkbook` |
| 加图表 | HTML 模板插 `<img src="data:image/svg+xml;base64,...">` 或引用 FR-07 数据 |
| 改 CSV 列 | `ReportExportServiceImpl.exportCsv` 表头数组 |
| 加 PDF | 引入 OpenPDF / iText 8 依赖,新增 endpoint |

---

## 五、v2 待办(简化版未覆盖的)

- [ ] `ExcelExportService` — 6 Sheet 原生 xlsx(EasyExcel)
- [ ] `PdfExportService` — iText 8 + 图表
- [ ] `REST Assured` 集成测试
- [ ] JUnit 5 单测覆盖率 ≥ 60%
- [ ] JaCoCo 报告
- [ ] GitHub Actions CI

---

## 六、参考资料

- **本次实现位置**:`backend/src/main/java/com/mavis/evalplatform/export/`
- **前端位置**:`frontend/src/views/export/ExportMain.vue`
- **E2E 脚本**:`D:\LJH\Project\test_*.js`
- **需求**:§3.8
- **架构**:§4.2.8
