# FR-03 问题输入与管理 — TODO

> **负责人:向锏楠**
> **关联分支**:`feature/model`(同 FR-02 一起)
> **预计工作量**:5 人日
> **对应评分层**:基础层

## 一、任务清单

### 1.1 Service(2 天)

- [ ] `QuestionService.create` — 校验 content 长度 ≤ 4000
- [ ] `QuestionService.update` / `delete` 软删除(deleted=1)
- [ ] `QuestionService.page` — 按 category、type 筛选
- [ ] `QuestionService.listMyLibrary` — 公共题库 + 个人题库
- [ ] `QuestionService.importBatch` — 用 EasyExcel 解析 Excel/CSV
  - 每行 1 题,字段:content, category, difficulty, type, expected_answer
  - 错误行号记录到 errorMessages
  - 限制 ≤ 200 题/次

### 1.2 Controller(0.5 天)

- [ ] 上传文件接口 `/api/questions/import`,接受 MultipartFile

### 1.3 Mapper(0.5 天)

- [ ] `QuestionMapper` 完整 SQL

### 1.4 数据库(0.5 天)

- [ ] V3.0__init_question.sql(question 表)

### 1.5 单元测试(1 天) — 覆盖 TC-03-001 ~ 005

- [ ] 单题输入
- [ ] 批量导入
- [ ] 批量导入格式错误
- [ ] 问题软删除
- [ ] 超长问题(>4000 字) → 400

## 二、业务规则(对齐需求规格说明书 §3.3.4)

- 单题 ≤ 4000 字
- 批量导入 ≤ 200 题/次
- 软删除(保留历史评测引用)

## 三、字段约束

| 字段 | 取值 |
|---|---|
| category | 学科,如"科学"、"编程"、"数学" |
| difficulty | 简单/中等/困难 |
| type | 事实/推理/创作/代码 |
| expected_answer | 可空,用于后续自动评测 |

## 四、CSV 导入示例(见需求规格说明书 §3.3.3)

```csv
id,category,difficulty,type,content,expected_answer
Q001,科学,中等,事实,什么是光合作用?,植物利用光能将二氧化碳和水转化为葡萄糖的过程
Q002,编程,简单,代码,写一个 Python 斐波那契函数,def fib(n): ...
```

## 五、参考资料

- 完整参考:`eval-platform-reference/backend/src/main/java/com/mavis/evalplatform/question/`
