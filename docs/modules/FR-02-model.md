# FR-02 模型配置管理 — TODO

> **负责人:向锏楠**
> **关联分支**:`feature/model`(合并 FR-02 + FR-03)
> **预计工作量**:5 人日
> **对应评分层**:提高层

## 一、任务清单

### 1.1 Service 层(2 天)

- [ ] `ModelService.page` — 分页查询,可按 provider 过滤
- [ ] `ModelService.listEnabled` — 评测页下拉用
- [ ] `ModelService.create` — 入参校验,API Key 用 `AesUtil.encrypt` 加密
- [ ] `ModelService.update` — 不允许改 provider
- [ ] `ModelService.toggleStatus` — 启用/停用
- [ ] `ModelService.delete` — 检查是否被评测引用(SELECT count FROM evaluation WHERE model_ids LIKE '%id%'),被引用抛 1001
- [ ] `ModelService.test` — 调用 `ModelAdapterFactory.getAdapter(provider).call(testRequest)`,返回结果与耗时

### 1.2 Controller(0.5 天)

- [ ] 完善 `ModelController` 的 `@PreAuthorize` 注解
- [ ] Swagger 注解补充

### 1.3 Mapper / SQL(0.5 天)

- [ ] `ModelConfigMapper` 完整 SQL,BaseMapper 已够用

### 1.4 数据库(0.5 天)

- [ ] 完善 `V2.0__init_model.sql`(`docs/modules/` 写明建表 SQL,由刘家豪合并到主 migration)

### 1.5 单元测试(1 天) — 覆盖 TC-02-001 ~ 006

- [ ] 正常新增
- [ ] API Key 加密存储(查 DB 验证是密文)
- [ ] 连接测试
- [ ] 错误 API Key
- [ ] 删除已使用模型 → 抛 1001
- [ ] 分页

## 二、业务规则(对齐需求规格说明书 §3.2.4)

- API Key 加密存储(AES-256),用 `common.util.AesUtil`
- 同一提供商可配置多个模型版本
- 被引用的模型不允许删除
- 连接测试:输入测试问题,验证 API 可用性

## 三、接口

| 方法 | 路径 | 权限 |
|---|---|---|
| GET | /api/models | ADMIN, ORGANIZER |
| GET | /api/models/enabled | ADMIN, ORGANIZER, SCORER |
| POST | /api/models | ADMIN |
| PUT | /api/models/{id} | ADMIN |
| POST | /api/models/{id}/toggle | ADMIN |
| DELETE | /api/models/{id} | ADMIN |
| POST | /api/models/test | ADMIN, ORGANIZER |

## 四、参考资料

- 完整参考:`eval-platform-reference/backend/src/main/java/com/mavis/evalplatform/model/`
- 需求:§3.2
- 架构:§4.2.2

## 五、接口契约(给其他模块用)

- `ModelService.listEnabled()` 返回所有 status=1 的模型(供评测页下拉)
- `ModelService.getById(id)` 供 evaluation 引用时获取
- **统一抛错**:`BusinessException(ErrorCode.MODEL_REFERENCED)` 当删除被引用时
