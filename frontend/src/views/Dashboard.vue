<script setup>
/**
 * 首页 Dashboard — 骨架
 * <p>
 * 由【靳磊】实现:可展示当前用户、最近评测、Kappa 概览等
 */
</script>

<template>
  <el-card shadow="never">
    <h2>欢迎来到「多模型回答对比与评测平台」🎉</h2>
    <p style="color: #606266; line-height: 1.7">
      本仓库是 <strong>架构骨架版</strong>,所有具体业务功能由各模块负责人按
      <code>docs/CONTRIBUTING.md</code> 完成。
    </p>
    <el-divider />
    <el-row :gutter="16">
      <el-col :span="8">
        <el-card shadow="hover">
          <h3>📋 阅读顺序</h3>
          <ol style="padding-left: 20px; line-height: 1.9">
            <li>根 README.md</li>
            <li>docs/ARCHITECTURE.md(必读)</li>
            <li>docs/CONTRIBUTING.md</li>
            <li>docs/modules/FR-XX-xxx.md(你的任务)</li>
          </ol>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <h3>👥 分工</h3>
          <ul style="padding-left: 20px; line-height: 1.9">
            <li>刘家豪(组长) — FR-01 / common / config</li>
            <li>向锏楠 — FR-02 / FR-03</li>
            <li>梁倩倩 — FR-04 / FR-07</li>
            <li>靳磊 — 前端整体</li>
            <li>宋子翔 — FR-05 / FR-06</li>
            <li>周文泽 — FR-08 / 测试</li>
          </ul>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <h3>🚀 一键启动</h3>
          <pre style="background: #f5f7fa; padding: 12px; border-radius: 4px; font-size: 12px"># 后端
cd backend
mvn spring-boot:run

# 前端(新窗口)
cd frontend
npm install
npm run dev</pre>
        </el-card>
      </el-col>
    </el-row>
  </el-card>
</template>
