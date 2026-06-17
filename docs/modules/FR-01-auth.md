# FR-01 用户登录与权限管理 — TODO

> **负责人:刘家豪**(已完成大部分实现,详见 `eval-platform-reference` 参考实现)
> **关联分支**:`feature/auth`
> **预计工作量**:8 人日
> **对应评分层**:基础层 + 提高层

## 状态:✅ 骨架完成,核心接口已定义,业务方法抛 TODO

参考实现位于 `../eval-platform-reference/backend/src/main/java/com/mavis/evalplatform/auth/`(本地参考,不提交)

## 一、任务清单

### 1.1 Service 层(2 天)

- [ ] `JwtService.init()` — 校验 secret ≥ 32 字节,初始化 key
- [ ] `JwtService.generateToken()` — 签发 HS256,Claims 含 uid/username/role
- [ ] `JwtService.parse()` — 解析,失败抛 JwtException
- [ ] `JwtService.isValid()` / `isExpired()` / `getExpireSeconds()`
- [ ] `UserService.findByUsername` / `findById` / `toUserInfo`
- [ ] `UserService.createUser` — BCrypt 加密 + 校验格式
- [ ] `UserService.changePassword` / `disableUser` / `page`
- [ ] `AuthService.login` — 完整流程:格式校验 → 查用户 → 锁检查 → 禁用检查 → 密码校验 → 失败累加 → 签发 token
- [ ] `AuthService.logout` — 写 audit_log
- [ ] `AuditLogService.logAsync` — 异步写库,失败仅 log
- [ ] `UserDetailsServiceImpl.loadUserByUsername` — 查 status=1,加 ROLE_ 前缀

### 1.2 Mapper / SQL(0.5 天)

- [ ] `UserMapper` 完整 SQL:selectByUsername / incrementFailedCount / resetFailedCount / disableUser
- [ ] `AuditLogMapper` 简单 select/insert

### 1.3 Controller / Filter(0.5 天)

- [ ] `JwtAuthenticationFilter.doFilterInternal` — 解析 token + 写 SecurityContext
- [ ] 完整单元测试 — 覆盖 TC-01-001 ~ TC-01-006

### 1.4 数据库(0.5 天)

- [ ] `V1.0__init_auth.sql` 已就绪,执行即可

### 1.5 集成测试(1 天)

- [ ] SpringBootTest 跑通完整登录流程
- [ ] 验证 JWT 在 SecurityConfig 链路下被正确识别

## 二、关键技术点

- BCrypt strength=10(耗时约 100ms)
- jjwt 0.12.x:`Jwts.builder().claims(...).signWith(key, Jwts.SIG.HS256).compact()`
- 失败计数 5 次 → 锁定 30 分钟 → locked_until 字段
- 审计日志用 `@Async("auditExecutor")` 异步写
- 公开端点:`/api/auth/login`、`/api/auth/register`、`/api/auth/refresh`
- 鉴权端点:其他所有路径

## 三、测试用例(对齐测试计划 §6.1)

| 用例 | 描述 | 状态 |
|---|---|---|
| TC-01-001 | 正常登录 | 待单测 |
| TC-01-002 | 密码错误 → 1003 | 待单测 |
| TC-01-003 | 连续 5 次失败 → 锁定 30 分钟 | 待单测 |
| TC-01-004 | Token 过期 → 401 | 待单测 |
| TC-01-005 | 角色越权 → 403 | 待集成测试 |
| TC-01-006 | 密码 BCrypt 存储 | 待单测 |

## 四、参考资料

- 完整参考实现:`D:\LJH\Project\eval-platform-reference\backend\...`
- 原始需求:`docs/01-需求规格说明书.docx` §3.1
- 架构设计:`docs/02-架构设计说明书.docx` §4.2.1
- 测试计划:`docs/03-测试计划说明书.docx` §6.1

## 五、完成后

- [ ] mvn test 全过
- [ ] 提 PR 到 develop
- [ ] 通知靳磊(前端)联调
- [ ] 站会同步进度
