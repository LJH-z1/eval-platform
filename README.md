# 多模型回答对比与评测平台

> **所有 8 个模块已完整实现** · 后端 + 前端 + 数据库 + 一键启动,开箱即用。
> 默认连真 MySQL 持久化,也可一键切到 H2 内存库做演示。

---

## 必读文档(按顺序)

1. **本 README.md** — 一句话了解项目 + 启动
2. **[`docs/ARCHITECTURE.md`](./docs/ARCHITECTURE.md)** — 架构总览、模块依赖、接口契约 ⭐
3. **[`docs/CONTRIBUTING.md`](./docs/CONTRIBUTING.md)** — 协作规范、分支、提交流程 ⭐
4. **[`docs/modules/FR-XX-xxx.md`](./docs/modules/)** — 每个模块的实现细节

---

## 一、当前状态(完整版)

| 模块 | 状态 | 负责人 |
|---|---|---|
| **FR-01 用户登录** | ✅ 完整实现(JWT + BCrypt + 限流 + 审计) | 刘家豪 |
| **FR-02 模型配置** | ✅ 完整实现(CRUD + AES-256 加密 + 5 provider) | 向锏楠 |
| **FR-03 问题管理** | ✅ 完整实现(CRUD + 批量导入 + 软删 + 20 测试通过) | 向锏楠 |
| **FR-04 评测任务** | ✅ 完整实现(5 OpenAI-compatible Adapter + 异步执行) | 梁倩倩 |
| **FR-05 多维评分** | ✅ 完整实现(4 维度 1-5 分 + UNIQUE 约束) | 宋子翔 |
| **FR-06 一致性分析** | ✅ 完整实现(Fleiss Kappa 4 维 + 排名) | 宋子翔 |
| **FR-07 成本统计** | ✅ 完整实现(总览 + 各模型 + 时序图 + CSV 导出) | 梁倩倩 |
| **FR-08 报告导出** | ✅ 完整实现(Excel CSV + HTML 双格式,6 章节) | 周文泽 |
| **前端整体** | ✅ LMArena 风格 + 8 模块完整页 + 顶部导航 | 靳磊 |
| **数据库** | ✅ MySQL(默认) + H2(可选),7 张表 | 刘家豪 |
| **文档** | ✅ 三大说明书 + ARCHITECTURE + CONTRIBUTING + 8 模块说明 | 刘家豪 |
| **一键启动** | ✅ `deploy/start-dev.{sh,bat}` | 刘家豪 |
| **MOCK 模式** | ✅ apiKey=`MOCK` 时返回模拟答案(无需真 API 即可演示) | — |
| CI | 待补(周文泽负责) | 周文泽 |

---

## 二、核心特性

### 2.1 MOCK 模式(零成本演示)

不接真模型 API 也能完整跑通"创建评测 → 异步运行 → 评分 → 看 Kappa → 导出报告"全流程。

**用法**:在「模型配置」页新建模型时:
- `API Key` 填 `MOCK`(或 `sk-MOCK`)
- `Endpoint` 填 `mock://m3`(或任意 `mock://` 开头的地址)

后端 `OpenAiCompatibleAdapter` 检测到 MOCK 模式,直接返回基于 provider 风格的模拟答案(含 200-800ms 模拟延迟 + token 估算),不发起任何 HTTP 请求。

### 2.2 5 大模型 Provider 适配器

所有 adapter 都走 OpenAI 兼容 chat/completions 协议,差异只在 endpoint:

| Provider | Endpoint | 默认模型 |
|---|---|---|
| M3(MiniMax) | `https://api.MiniMax.chat/v1/text/chatcompletion_v2` | MiniMax-Text-01 |
| OpenAI | `https://api.openai.com/v1/chat/completions` | gpt-4o-mini |
| 智谱 GLM | `https://open.bigmodel.cn/api/paas/v4/chat/completions` | glm-4-plus |
| 通义千问 | `https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions` | qwen-max |
| 月之暗面 Kimi | `https://api.moonshot.cn/v1/chat/completions` | moonshot-v1-8k |

新增 Provider 只需加一个 adapter 类继承 `OpenAiCompatibleAdapter`。

### 2.3 LMArena 风格前端

- 顶部 emoji 导航 + 渐变 logo
- 评测页用 Arena 风格(盲测对比 + 投票)
- 排行榜用渐变表头 + 排名 1/2/3 分色(金/银/铜)
- 评测详情:并排展示多模型回答 + 成本/耗时统计

---

## 三、本地启动

### 3.1 方式 A:用真 MySQL(推荐,数据持久化)

**前提**:本机已装 MySQL 8.0(服务已启动)

**步骤 1 — 启 MySQL 服务**:
```powershell
# 管理员 PowerShell
Set-Service MySQL80 -StartupType Automatic
Start-Service MySQL80
```

**步骤 2 — 建库 + 跑初始化 SQL**:
```powershell
cd D:\LJH\Project\eval-platform
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -uroot -p123456 -e "CREATE DATABASE IF NOT EXISTS eval_platform DEFAULT CHARACTER SET utf8mb4;"
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -uroot -p123456 eval_platform < backend\src\main\resources\db\migration\V1.0__init.sql
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -uroot -p123456 eval_platform -e "INSERT INTO user (id, username, password, email, role, status) VALUES (1, 'admin', '\$2a\$10\$Oe33tH5qPJRTuWzpvAbLxeiIibqfZ.YwV/X3AQGCWPDoOP2UnORGy', 'admin@test.local', 'ADMIN', 1), (2, 'org1', '\$2a\$10\$Oe33tH5qPJRTuWzpvAbLxeiIibqfZ.YwV/X3AQGCWPDoOP2UnORGy', 'org1@test.local', 'ORGANIZER', 1);"
```

(密码不是 123456 的话自己改。BCrypt hash 对应密码 `admin123`)

**步骤 3 — 启后端**(端口 8082,连 MySQL):
```powershell
java -jar backend\target\eval-platform.jar --server.port=8082
```

**步骤 4 — 启前端**(另开终端):
```powershell
cd frontend
npm install
npm run dev
```

**步骤 5 — 浏览器**:http://localhost:5173 → 用 `admin` / `admin123` 登录

### 3.2 方式 B:用 H2 内存库(零依赖,数据每次重启清空)

```powershell
# 启后端(用 dev profile)
java -jar backend\target\eval-platform.jar --server.port=8082 --spring.profiles.active=dev

# 启前端
cd frontend
npm run dev
```

> H2 模式下 admin / org1 自动 seed(密码 `admin123`)。
> **数据不持久化**,后端重启就清空。

### 3.3 方式 C:一键脚本

```powershell
deploy\start-dev.bat       # Windows
bash deploy/start-dev.sh   # Linux / macOS / WSL
```

---

## 四、端口 + 账号

| 端口 | 用途 |
|---|---|
| 3306 | MySQL(用方式 A 时) |
| 8082 | 后端(Spring Boot + Tomcat) |
| 5173 | 前端(Vite dev server) |

**默认账号**:
- `admin` / `admin123`(管理员,可看所有页)
- `org1`  / `admin123`(组织者,可建评测/模型)

**测试场景建议路径**:
1. `模型配置` → 新建 2 个 MOCK 模型(apiKey=MOCK, endpoint=mock://)
2. `问题管理` → 新建 2-3 个问题(分类/题型/难度都对)
3. `评测任务` → 新建评测,选 2 模型 × 2 问题 → 提交
4. 自动跳到详情页,看 PENDING → RUNNING → COMPLETED(MOCK 4-5 秒跑完)
5. `评分` → 4 个维度都打 3-5 分 → 提交
6. `一致性分析` → 看 Fleiss Kappa 4 维度 + 模型排名
7. `成本统计` → 看总览/各模型/时序图 + 下载 CSV
8. `报告导出` → 下载 Excel(CSV)和 HTML 报告

---

## 五、技术栈

| 类别 | 技术 | 版本 |
|---|---|---|
| 后端 | Spring Boot | 3.2.x |
| 后端 | Spring Security + jjwt | 6.x / 0.12.x |
| 后端 | MyBatis-Plus | 3.5.x |
| 后端 | MySQL / H2(可选) | 8.0+ / 2.x |
| 后端 | Redis(可选,缺时降级) | 7.x |
| 前端 | Vue 3 + Vite | 3.4 / 5+ |
| 前端 | Element Plus + Pinia | 2.7+ / 2.x |
| 前端 | Axios | 1.x |
| 文档 | SpringDoc OpenAPI | 2.3.x |

---

## 六、API 端点速查

```
POST   /api/auth/login
POST   /api/auth/logout
GET    /api/auth/me
POST   /api/auth/register
POST   /api/auth/change-password

GET    /api/users                              # 分页
POST   /api/users/{id}/disable                 # 禁用

GET    /api/models                             # 列表
POST   /api/models                             # 新建
GET    /api/models/enabled                     # 已启用(下拉用)
GET    /api/models/{id}
PUT    /api/models/{id}
DELETE /api/models/{id}                        # 引用则 1023
POST   /api/models/{id}/toggle                 # 启用/停用
POST   /api/models/test                        # 连接测试

GET    /api/questions                          # 分页 + 筛选
POST   /api/questions                          # 新建
GET    /api/questions/{id}
PUT    /api/questions/{id}
DELETE /api/questions/{id}                     # 软删
GET    /api/questions/library                  # 公共/个人题库
POST   /api/questions/import                    # 批量导入 CSV

POST   /api/evaluations                        # 新建
POST   /api/evaluations/{id}/run               # 异步启动
GET    /api/evaluations/{id}                   # 详情(含 answers)
GET    /api/evaluations                        # 分页
GET    /api/evaluations/{id}/answers
DELETE /api/evaluations/{id}

POST   /api/scores                             # 提交评分(UNIQUE 约束)
GET    /api/scores/by-evaluation?evaluationId=
GET    /api/scores/by-answer/{answerId}
GET    /api/scores/check?answerId=

GET    /api/stats/kappa?evaluationId=           # 4 维 Fleiss Kappa
GET    /api/stats/controversial?evaluationId=  # 争议项
GET    /api/stats/scorer-ranking?evaluationId=
GET    /api/stats/model-ranking?evaluationId=

GET    /api/billing/summary?evaluationId=
GET    /api/billing/time-series?evaluationId=&granularity=hour|day
GET    /api/billing/by-model?evaluationId=
GET    /api/billing/platform-summary
GET    /api/billing/export?evaluationId=       # CSV 下载

GET    /api/export/{id}/excel                  # CSV 下载
GET    /api/export/{id}/pdf                    # HTML 下载
GET    /api/export/{id}/meta
```

Swagger UI:http://localhost:8082/swagger-ui.html

---

## 七、目录结构

```
eval-platform/
├── backend/                              # Spring Boot 后端
│   ├── src/main/java/com/mavis/evalplatform/
│   │   ├── common/                       # 公共(异常/Result/AOP/AesUtil/PageResult)
│   │   ├── config/                       # MyBatisPlus/Security/Cors/Executor
│   │   ├── auth/                         # FR-01 用户登录
│   │   ├── model/                        # FR-02 模型配置
│   │   ├── question/                     # FR-03 问题管理
│   │   ├── evaluation/                   # FR-04 评测任务(含 adapter)
│   │   ├── score/                        # FR-05 多维评分
│   │   ├── stats/                        # FR-06 一致性分析
│   │   ├── billing/                      # FR-07 成本统计
│   │   └── export/                       # FR-08 报告导出
│   └── src/main/resources/
│       ├── application.yml               # 默认 profile (prod → MySQL)
│       ├── application-dev.yml           # dev profile (H2 内存)
│       ├── application-prod.yml          # prod profile (MySQL)
│       └── db/migration/V1.0__init*.sql
├── frontend/                             # Vue 3 前端
│   ├── src/views/
│   │   ├── Dashboard.vue                 # 首页(LMArena 风格)
│   │   ├── arena/Arena.vue               # 盲测对比
│   │   ├── auth/                         # Login / Register
│   │   ├── model/                        # FR-02
│   │   ├── question/                     # FR-03
│   │   ├── evaluation/                   # FR-04
│   │   ├── score/                        # FR-05
│   │   ├── stats/                        # FR-06
│   │   ├── billing/                      # FR-07
│   │   └── export/                       # FR-08
│   └── src/api/                           # 28+ API 包装
├── deploy/                                # 一键启动脚本
├── docs/
│   ├── 01-需求规格说明书.docx
│   ├── 02-架构设计说明书.docx
│   ├── 03-测试计划说明书.docx
│   ├── ARCHITECTURE.md
│   ├── CONTRIBUTING.md
│   └── modules/                          # 8 个模块说明
├── .github/                               # PR/Issue 模板
├── .gitignore
└── README.md
```

---

## 八、Git 提交建议

```bash
# 首次提交
git add .
git commit -m "feat: 完整实现 8 模块(FR-01~08)+ MySQL 持久化 + LMArena 风格前端 + MOCK 模式"

# 之后按模块分提交
git commit -m "feat(FR-02): 模型配置 AES-256 加密 + 5 provider + MOCK 模式"
git commit -m "feat(FR-04): 评测任务 + 5 OpenAI-compatible adapter + 异步执行"
git commit -m "feat(FR-05): 多维评分 + UNIQUE(answer, scorer) 约束"
git commit -m "feat(FR-06): Fleiss Kappa 4 维度 + 排名 + 争议项识别"
git commit -m "feat(FR-07): 成本统计 + 时序图 + CSV 导出"
git commit -m "feat(FR-08): 报告导出(Excel CSV + HTML 双格式 6 章节)"
git commit -m "feat(frontend): LMArena 风格 + 顶部导航 + 8 模块完整页"
```

---

## 九、测试账号 / 数据(可选)

```sql
-- 评分员(用于测试多评分员场景)
INSERT INTO user (id, username, password, email, role, status) VALUES
  (3, 'scorer1', '$2a$10$Oe33tH5qPJRTuWzpvAbLxeiIibqfZ.YwV/X3AQGCWPDoOP2UnORGy', 's1@test.local', 'SCORER', 1);

-- MOCK 模型(用于无 API Key 演示)
INSERT INTO model_config (name, provider, api_key, endpoint, model_version, status) VALUES
  ('M3 演示', 'M3', 'MOCK', 'mock://m3', 'MiniMax-Text-01', 1),
  ('Qwen 演示', 'QWEN', 'MOCK', 'mock://qwen', 'qwen-max', 1);
```

---

## 十、许可证

课程项目,仅供学习。
