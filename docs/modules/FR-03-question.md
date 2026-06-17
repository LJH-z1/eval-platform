# FR-03 问题输入与管理 — ✅ 已实现(基础版)

> **负责人:向锏楠**
> **关联分支**:`feature/model`(同 FR-02 一起)
> **预计工作量**:5 人日
> **实际状态**:后端 + 前端 + 测试 + 一键启动 全部就绪

> ⚠️ 本实现已在 `eval-platform` 骨架仓库直接补全,**无需切换到独立分支**。
> 如果向锏楠后续有自己的实现思路,可以在 feature/question-v2 分支上重写,字段和端点保持一致即可前后端兼容。

---

## 一、任务完成清单

### 1.1 Service(2 天)

- [x] `QuestionService.create` — 校验 content 长度 ≤ 4000
- [x] `QuestionService.update` / `delete` 软删除(`deleted=1`,通过 `UpdateWrapper` 绕过 MyBatis-Plus 逻辑删除拦截)
- [x] `QuestionService.page` — 按 category、type、difficulty、isPublic 4 维筛选 + 关键字(content / expectedAnswer / category LIKE)
- [x] `QuestionService.listForLibrary` — 公共题库 + 个人题库(按 userId 过滤)
- [x] `QuestionService.importBatch` — 用 UTF-8 手写 CSV 解析(无外部依赖),错误行号记录到 `errorMessages`,限制 ≤ 200 题/次

### 1.2 Controller(0.5 天)

- [x] 上传文件接口 `POST /api/questions/import`,接受 `MultipartFile`
- [x] `POST /api/questions` 创建
- [x] `PUT /api/questions/{id}` 更新
- [x] `DELETE /api/questions/{id}` 软删
- [x] `GET /api/questions/{id}` 详情
- [x] `GET /api/questions` 分页查询

### 1.3 Mapper(0.5 天)

- [x] `QuestionMapper extends BaseMapper<Question>`,无自定义 SQL(MyBatis-Plus `LambdaQueryWrapper` 满足所有筛选)

### 1.4 数据库(0.5 天)

- [x] `question` 表已在 `V1.0__init.sql` 创建(MySQL 8.0)
- [x] `V1.0__init-h2.sql` 为 dev profile 创建兼容版本(`NON_KEYWORDS=user`)
- [x] 字段:`id, category, difficulty, type, content, expected_answer, is_public, deleted, created_by, create_time, update_time`

### 1.5 单元测试(1 天) — 覆盖 TC-03-001 ~ 005

- [x] TC-03-001 单题输入成功(`create_正常参数返回QuestionVO`)
- [x] TC-03-001 单题输入失败(content 超 4000 字 → 400 / null)
- [x] TC-03-002 批量导入成功(`importBatch_合法CSV返回成功结果`)
- [x] TC-03-003 批量导入格式错误(`importBatch_格式错误返回错误行号`)
- [x] TC-03-003 批量导入超限(>200 题 → IllegalArgumentException)
- [x] TC-03-004 问题软删除(`delete_更新deleted为1`)
- [x] TC-03-004 已删除题目不可重复删除(`delete_已删除题目返回false`)
- [x] TC-03-005 更新成功(`update_正常参数更新成功`)
- [x] TC-03-005 更新失败(不存在题目返回 false)
- [x] 详情查询(`getById_正常返回`)

> **结果**:`mvn test -o` 全部 20 个测试通过(0 failures, 0 errors, 0 skipped)。

### 1.6 前端(配套完成)

- [x] `QuestionList.vue` — 列表 + 4 维筛选 + 关键字搜索 + 分页 + 删除按钮
- [x] `QuestionForm.vue` — 新建 / 编辑共用表单,带实时 Markdown 预览
- [x] `QuestionImport.vue` — CSV 上传 + 结果明细(成功/失败数 + 错误行号列表)
- [x] `router/index.js` 路由:`/question`, `/question/new`, `/question/:id/edit`, `/question/import`
- [x] `Layout.vue` 菜单:"问题管理 → 题目列表 / 新建问题 / 批量导入"

---

## 二、业务规则(对齐需求规格说明书 §3.3.4)

| 规则 | 实现位置 |
|---|---|
| 单题 ≤ 4000 字 | `QuestionServiceImpl.validate()` 抛 `BusinessException(400)` |
| 批量导入 ≤ 200 题/次 | `QuestionServiceImpl.importBatch()` 抛 `IllegalArgumentException` |
| 软删除(保留历史评测引用) | `QuestionServiceImpl.delete()` 使用 `UpdateWrapper.set("deleted", 1)` 软更新 |
| 仅创建者可删除 / 编辑 | `@PreAuthorize("hasAnyRole('ADMIN','ORGANIZER','SCORER')")` |
| 个人 / 公共题库隔离 | `listForLibrary(userId, onlyMine)` 按 `createdBy` 过滤 |

---

## 三、字段约束

| 字段 | 取值 |
|---|---|
| `category` | 学科,如"科学"、"编程"、"数学" |
| `difficulty` | 简单 / 中等 / 困难(枚举 `Difficulty`) |
| `type` | 事实 / 推理 / 创作 / 代码(枚举 `QuestionType`) |
| `expectedAnswer` | 可空,用于后续自动评测 |
| `isPublic` | true(公共题库,所有人可见)/ false(仅创建者可见) |
| `createdBy` | 创建者 userId(自 `auth.user.id`) |

---

## 四、CSV 导入示例(见需求规格说明书 §3.3.3)

```csv
id,category,difficulty,type,content,expected_answer
Q001,科学,中等,事实,什么是光合作用?,植物利用光能将二氧化碳和水转化为葡萄糖的过程
Q002,编程,简单,代码,写一个 Python 斐波那契函数,def fib(n): ...
```

> **注意**:第 1 列 `id` 在导入时被忽略(数据库自增主键);实际写入字段是 `category, difficulty, type, content, expected_answer, is_public`。

---

## 五、API 端点契约(后端实际响应)

| 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|
| `POST` | `/api/questions` | ADMIN/ORGANIZER/SCORER | 创建题目,返回 QuestionVO |
| `PUT` | `/api/questions/{id}` | ADMIN/ORGANIZER/SCORER | 更新题目 |
| `DELETE` | `/api/questions/{id}` | ADMIN/ORGANIZER | 软删除 |
| `GET` | `/api/questions/{id}` | 任意已登录 | 详情 |
| `GET` | `/api/questions` | 任意已登录 | 分页(`category, type, difficulty, isPublic, keyword, page, size`) |
| `GET` | `/api/questions/library` | 任意已登录 | 个人题库 / 公共题库 |
| `POST` | `/api/questions/import` | ADMIN/ORGANIZER | CSV 批量导入,返回 `{successCount, failCount, errorMessages}` |

---

## 六、端到端验证(已在 dev profile 跑通)

```
✅ POST /api/auth/login  admin/admin123   → 200,token=eyJ...
✅ GET  /api/questions     (无参数)       → 200, total=0
✅ POST /api/questions     (新建)         → 200, id=1
✅ GET  /api/questions     (1 条)         → 200, total=1
✅ GET  /api/questions/1   (详情)         → 200
✅ PUT  /api/questions/1   (更新)         → 200
✅ DELETE /api/questions/1 (软删)         → 200
✅ GET  /api/questions     (软删生效)     → 200, total=0
✅ POST /api/evaluations   (FR-04 占位)   → 501 TODO
✅ POST /api/auth/login  (wrong pwd)      → 200, code=1003
```

启动方式:见 `README.md §3.4` 或 `deploy/start-dev.{sh,bat}`。

---

## 七、负责人修改指南

如果你(向锏楠)有自己的实现思路,只需注意以下几点保持前后端兼容:

| 改的东西 | 要同步改的地方 |
|---|---|
| 加字段 | `Question.java` 实体 + `QuestionRequest.java` 入参 + `QuestionVO.java` 出参 + `QuestionServiceImpl.applyRequest()` + `V1.0__init.sql` 数据库 |
| 改校验规则 | `QuestionServiceImpl.validate()` |
| 改筛选维度 | `QuestionServiceImpl.page()` + `QuestionList.vue` 搜索栏 |
| 改字段类型 | 数据库 ALTER TABLE + 实体属性 + DTO 同步 |
| 换编辑器(富文本) | 在 `QuestionForm.vue` 引入 `@tinymce/tinymce-vue`,替换 textarea |

---

## 八、参考资料

- **完整参考**:`D:\LJH\Project\eval-platform-reference\backend\src\main\java\com\mavis\evalplatform\question\`(本地参考,不上传 GitHub)
- **本次实现位置**:`backend/src/main/java/com/mavis/evalplatform/question/`
- **前端位置**:`frontend/src/views/question/`
- **测试位置**:`backend/src/test/java/com/mavis/evalplatform/question/service/QuestionServiceTest.java`
