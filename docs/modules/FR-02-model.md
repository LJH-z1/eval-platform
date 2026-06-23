# FR-02 模型配置管理 — ✅ 已实现

> **负责人:向锏楠**
> **关联分支**:`feature/model`(合并 FR-02 + FR-03)
> **预计工作量**:5 人日
> **实际状态**:后端 + 前端 + 端到端验证 全部就绪
> **对应评分层**:提高层

---

## 一、任务完成清单

### 1.1 Service(2 天)

- [x] `ModelServiceImpl.page` — 分页查询,支持 `provider / keyword / status` 三维筛选
- [x] `ModelServiceImpl.listEnabled` — `status=1` 列表,评测页下拉专用
- [x] `ModelServiceImpl.create` — 入参校验(name ≤ 50、provider 限定 7 个枚举),`apiKey` 走 `AesUtil.encrypt` 加密入库
- [x] `ModelServiceImpl.update` — 不允许改 `provider`(字段拷贝时跳过),`apiKey` 为空则保留原值
- [x] `ModelServiceImpl.toggleStatus` — 状态 0/1 翻转
- [x] `ModelServiceImpl.delete` — 检查 `answer` 表引用,被引用抛 `BusinessException(1001)`
- [x] `ModelServiceImpl.test` — 调用 `ModelAdapterFactory.getAdapter(provider).call()`,返回结果 + 耗时 + 错误码

### 1.2 Controller(0.5 天)

- [x] `ModelController` 全部端点 + `@PreAuthorize` 注解
  - `GET /api/models` — ADMIN/ORGANIZER
  - `GET /api/models/enabled` — 任意已登录
  - `POST /api/models` — ADMIN
  - `PUT /api/models/{id}` — ADMIN
  - `POST /api/models/{id}/toggle` — ADMIN
  - `DELETE /api/models/{id}` — ADMIN
  - `POST /api/models/test` — ADMIN/ORGANIZER

### 1.3 Mapper(0.5 天)

- [x] `ModelConfigMapper extends BaseMapper<ModelConfig>`,复杂查询全部走 `LambdaQueryWrapper`

### 1.4 数据库(0.5 天)

- [x] `V1.0__init.sql` 中建表 `model_config`:
  - 字段:`id, name, provider, api_key(加密), endpoint, model_version, temperature, top_p, max_tokens, price_per_k, status, created_at, updated_at, created_by`
  - 索引:`UNIQUE(name)`,`idx_provider`,`idx_status`
  - 枚举 `provider`:M3 / ZHIPU / QWEN / WENXIN / KIMI / OPENAI / CUSTOM

### 1.5 端到端验证(已在 MySQL prod 跑通)

```
✅ POST /api/auth/login              → 200, token
✅ GET  /api/models/enabled          → 200, []
✅ POST /api/models (新建)           → 200, id=17, name=Test, provider=CUSTOM
✅ GET  /api/models/{id}             → 200, apiKeyMasked="****"
✅ POST /api/models (name=Test 重复)  → 200, code=1021, msg=模型名称已被占用
✅ POST /api/models (provider=自定义) → 200, code=1022, msg=不支持的提供商
✅ POST /api/models (MOCK 模式)      → 200, modelVersion=M3-Plus
✅ POST /api/models/test (MOCK)      → 200, mock 答: "【MOCK-M3】 这是一段模拟回答..."
```

### 1.6 前端(配套完成)

- [x] `ModelList.vue` — 表格 + 启用开关 + 删除按钮(provider 不可改)
- [x] `ModelForm.vue` — 新建/编辑共用,provider 切换时自动套用 endpoint 模板,`MOCK` 提示
- [x] `router/index.js`:`/model`, `/model/new`, `/model/:id/edit`
- [x] `Layout.vue` 菜单:"模型管理 → 模型列表 / 新建模型"

---

## 二、业务规则(对齐需求规格说明书 §3.2.4)

| 规则 | 实现位置 |
|---|---|
| API Key 加密存储(AES) | `ModelServiceImpl.create/update` 调用 `AesUtil.encrypt`,返回 VO 时 mask 成 `****` |
| name 唯一 | `V1.0__init.sql` UNIQUE 约束,DB 抛异常被 `GlobalExceptionHandler` 捕获转 `1021` |
| provider 不可改 | `ModelServiceImpl.update` 拷贝字段时跳过 `provider` |
| 状态翻转 | `ModelServiceImpl.toggleStatus(id)` |
| 被引用不可删 | `ModelServiceImpl.delete` 先 `count(answer where model_id=id)`,>0 抛 `1001` |
| 连接测试 | `ModelServiceImpl.test` 走 `ModelAdapterFactory`,5s 超时 |

---

## 三、接口契约(给其他模块用)

| 方法 | 调用方 | 返回 |
|---|---|---|
| `ModelService.listEnabled()` | FR-04 evaluation 页面 | `List<ModelConfig>`(status=1) |
| `ModelService.getById(id)` | FR-04/05/07/08 | `ModelConfig`(apiKey 已 mask) |
| `ModelService.test(request)` | 管理页 | `{success, content, latencyMs, errorCode}` |

---

## 四、负责人修改指南

| 改的东西 | 要同步改的地方 |
|---|---|
| 加 provider | `ErrorCode.validateProvider` 白名单 + `ModelAdapterFactory` 注册 + `ModelForm.vue` 加 `<el-option>` |
| 改字段 | `ModelConfig.java` + `ModelRequest.java` + `V1.0__init.sql` |
| 换 AES key | `application.yml` `eval.aes.key` 重生成(旧数据将无法解密,需迁移) |
| 改连接测试超时 | `ModelAdapterFactory` 默认 timeout 字段 |

---

## 五、参考资料

- **本次实现位置**:`backend/src/main/java/com/mavis/evalplatform/model/`
- **前端位置**:`frontend/src/views/model/`
- **依赖**:`AesUtil` (`common/util/`)、`ModelAdapterFactory` (`evaluation/adapter/`)
