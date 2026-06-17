# FR-08 报告导出与自动化测试 — TODO

> **负责人:周文泽**(测试负责人)
> **关联分支**:`feature/test`
> **预计工作量**:8 人日
> **对应评分层**:基础层 + 提高层

## 一、任务清单

### 1.1 Excel 导出(1.5 天)

- [ ] `ReportExportService.exportExcel` — 用 EasyExcel
- [ ] 6 个 Sheet:
  1. 封面(评测名/创建者/创建时间/模型数/问题数)
  2. 评测概述
  3. 模型调用统计(各模型总 token / 总费用 / 平均耗时)
  4. 评分统计(各模型各维度平均分)
  5. 一致性分析(Kappa 值)
  6. 详细结果(问题 × 模型 × 评分)

### 1.2 PDF 导出(2 天)

- [ ] `ReportExportService.exportPdf` — iText 8
- [ ] 6 个章节(对齐架构设计说明书 §4.2.8)
- [ ] 包含 ECharts 图表(用 Flying Saucer 渲染 HTML 转 PDF)

### 1.3 单元测试(2 天)

- [ ] `ExcelExportTest` — 验证每个 Sheet 数据正确
- [ ] `PdfExportTest` — 验证 PDF 生成
- [ ] 文件名格式:评测报告_{名称}_{YYYYMMDD}.xlsx

### 1.4 自动化测试体系(2 天)

- [ ] REST Assured 集成测试 — 覆盖登录、调用、评分、导出
- [ ] JUnit 5 各模块 Service 单测覆盖率 ≥ 60%
- [ ] JaCoCo 生成覆盖率报告
- [ ] GitHub Actions CI 配置(`.github/workflows/ci.yml`):
  - 触发:push 到 develop / PR
  - 步骤:checkout → setup-java → cache m2 → mvn test → mvn jacoco:report → 上传 artifact

### 1.5 缺陷管理(0.5 天)

- [ ] GitHub Issues 模板(已提供 .github/ISSUE_TEMPLATE/bug_report.md)
- [ ] 飞书任务表每日更新

## 二、报告文件名格式

```
评测报告_{评测名}_{YYYYMMDD}.xlsx
评测报告_{评测名}_{YYYYMMDD}.pdf
```

例:评测报告_M3 vs GLM 评测_20260617.xlsx

## 三、测试覆盖率要求(对齐测试计划 §9.2)

- Service 层 ≥ 60%
- 关键接口(登录、调用、评分)100% 覆盖
- 必测用例 100% 通过

## 四、CI 配置文件示例

`.github/workflows/ci.yml`:

```yaml
name: CI
on:
  push:
    branches: [develop, main]
  pull_request:
    branches: [develop]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven
      - name: Run tests
        run: mvn -B test
      - name: Generate coverage
        run: mvn -B jacoco:report
      - name: Upload coverage
        uses: actions/upload-artifact@v4
        with:
          name: coverage-report
          path: backend/target/site/jacoco/
```

## 五、参考资料

- 完整参考:`eval-platform-reference/backend/src/main/java/com/mavis/evalplatform/export/`
- 需求:§3.8
- 架构:§4.2.8
