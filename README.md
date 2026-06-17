# 多模型回答对比与评测平台

> ⚠️ **本仓库是架构骨架版**,所有具体业务实现由各模块负责人按 `docs/CONTRIBUTING.md` 完成。
> 完整参考实现位于 `D:\LJH\Project\eval-platform-reference\(本地参考,不上传 GitHub)。

---

## 必读文档(按顺序)

1. **本 README.md** — 一句话了解项目 + 启动
2. **[`docs/ARCHITECTURE.md`](./docs/ARCHITECTURE.md)** — 架构总览、模块依赖、接口契约 ⭐
3. **[`docs/CONTRIBUTING.md`](./docs/CONTRIBUTING.md)** — 协作规范、分支、提交流程 ⭐
4. **[`docs/modules/FR-XX-xxx.md`](./docs/modules/)** — 你的具体任务清单

---

## 一、当前状态(架构骨架版)

| 模块 | 状态 | 负责人 |
|---|---|---|
| FR-01 用户登录 | ✅ 完整实现(JWT + BCrypt + 限流 + 审计) | 刘家豪 |
| FR-02 模型配置 | 接口契约 + 实体 + Controller 占位 | 向锏楠 |
| **FR-03 问题管理** | ✅ **完整实现**(CRUD + 批量导入 + 软删 + 20 测试通过) | **向锏楠** |
| FR-04 评测 + 适配器 | 接口契约 + 实体 + 5 个 Adapter 占位 | 梁倩倩 |
| FR-05 多维评分 | 接口契约 + 实体 + Controller 占位 | 宋子翔 |
| FR-06 Fleiss Kappa | 接口契约 + Controller 占位 | 宋子翔 |
| FR-07 成本统计 | 接口契约 + Controller 占位 | 梁倩倩 |
| FR-08 报告导出 | 接口契约 + Controller 占位 | 周文泽 |
| 前端整体 | ✅ 路由 + 公共组件 + FR-01/FR-03 完整页 + 其余 6 个模块占位 | 靳磊 |
| 数据库 | V1.0__init.sql(7 张表) + V1.0__init-h2.sql(dev profile) | 刘家豪 |
| 文档 | 三大说明书 + ARCHITECTURE + CONTRIBUTING + 8 个模块 TODO | 刘家豪 |
| 一键启动 | ✅ `deploy/start-dev.{sh,bat}`(H2 内存库,无需 MySQL) | 刘家豪 |
| CI | 待补(周文泽负责) | 周文泽 |

**调用约定**:已完成模块(FR-01、FR-03)是真实现;其余模块的 Service 方法返回 `Result.error(501, "TODO")`,Controller 占位但不会让 Spring DI 报错,可以正常登录、看问题管理页、调用 FR-02/04/05/06/07/08 占位端点看到友好的 "TODO" 提示。

---

## 二、目录结构

```
eval-platform/
├── backend/                              # Spring Boot 后端
├── frontend/                             # Vue 3 前端
├── deploy/
├── docs/
│   ├── 01-需求规格说明书.docx
│   ├── 02-架构设计说明书.docx
│   ├── 03-测试计划说明书.docx
│   ├── ARCHITECTURE.md                   # ⭐ 架构总览
│   ├── CONTRIBUTING.md                   # ⭐ 协作指南
│   └── modules/
│       ├── FR-01-auth.md
│       ├── FR-02-model.md
│       ├── FR-03-question.md
│       ├── FR-04-evaluation.md
│       ├── FR-05-score.md
│       ├── FR-06-stats.md
│       ├── FR-07-billing.md
│       └── FR-08-export.md
├── .github/
│   ├── ISSUE_TEMPLATE/
│   └── PULL_REQUEST_TEMPLATE.md
├── .gitignore
└── README.md
```

---

## 三、本地启动

### 3.1 启动数据库(用 MySQL 客户端执行)

```bash
mysql -uroot -p < backend/src/main/resources/db/migration/V1.0__init.sql
```

> 骨架版不再预置 admin 账号,由各负责人在自己实现时根据需要添加。

### 3.2 启动后端

```bash
cd backend
mvn spring-boot:run          # 第一次会下载依赖
```

后端启动后:
- 主页:http://localhost:8080
- Swagger UI:http://localhost:8080/swagger-ui.html

### 3.3 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端地址:http://localhost:5173

### 3.4 一键启动(无需 MySQL,用 H2 内存库)

```bash
# Windows
deploy\start-dev.bat

# Linux / macOS / WSL
bash deploy/start-dev.sh
```

脚本会:
1. 用 `dev` profile 启动 Spring Boot(连接内嵌 H2)
2. 自动 seed 一条 admin 账号(`admin` / `admin123`)
3. 启动 Vue 前端 dev server
4. 浏览器打开 **http://localhost:5173**,登录后即可看到 **FR-01 用户管理 + FR-03 问题管理** 完整页面

> 想用真实 MySQL?改用 `mvn spring-boot:run`(默认 profile),然后 `mysql -uroot -p < backend/src/main/resources/db/migration/V1.0__init.sql`。

---

## 四、上传 GitHub 时的建议

### 4.1 初始化仓库

```bash
# 在 GitHub 上创建 eval-platform-team/eval-platform 仓库
cd eval-platform
git init
git remote add origin git@github.com:eval-platform-team/eval-platform.git
git checkout -b main

# 首次提交
git add .
git commit -m "feat: 初始化项目骨架(后端 + 前端 + 文档 + 数据库)"
git push -u origin main
```

### 4.2 分支保护

- 在 GitHub 仓库 Settings → Branches 添加规则
- `main` 分支:Require pull request reviews before merging(至少 1 人:刘家豪)
- `develop` 分支:Require pull request reviews before merging(任意组员)

### 4.3 邀请协作者

Settings → Collaborators → 邀请:
- 向锏楠、靳磊、梁倩倩、宋子翔、周文泽

### 4.4 GitHub Actions(待补,周文泽负责)

`.github/workflows/ci.yml` 模板见 `docs/modules/FR-08-export.md` §四。

---

## 五、技术栈

| 类别 | 技术 | 版本 |
|---|---|---|
| 后端 | Spring Boot | 3.2.x |
| 后端 | Spring Security + jjwt | 6.x / 0.12.x |
| 后端 | MyBatis-Plus | 3.5.x |
| 后端 | MySQL / Redis | 8.0+ / 7.x |
| 前端 | Vue 3 + Vite | 3.4 / 5+ |
| 前端 | Element Plus + Pinia | 2.7+ / 2.x |
| 前端 | Axios | 1.x |
| 文档 | SpringDoc OpenAPI | 2.3.x |

---

## 六、许可证

课程项目,仅供学习。
