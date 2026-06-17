<script setup>
/**
 * 通用占位页 — 模块未实现
 * <p>
 * 用法:每个具体模块的页面,在该模块负责人完成前都用本组件占位
 * <p>
 * 路由:'/model/list'  →  ModulePlaceholder({ module: 'FR-02', owner: '向锏楠', title: '模型配置管理' })
 */
defineProps({
  module:   { type: String, required: true },   // FR-01 ~ FR-08
  owner:    { type: String, required: true },   // 谁负责
  title:    { type: String, required: true },
  features: { type: Array,  default: () => [] } // 本页要实现的功能
})
</script>

<template>
  <el-card shadow="never">
    <el-result icon="info" :title="title" :sub-title="`${module} · 由 ${owner} 负责实现`">
      <template #extra>
        <el-tag type="warning">{{ module }}</el-tag>
        <el-tag type="primary" style="margin-left: 8px">负责人:{{ owner }}</el-tag>
      </template>
    </el-result>

    <el-divider content-position="left">本页面将实现的功能</el-divider>
    <el-empty v-if="features.length === 0" description="暂无功能清单" />
    <ul v-else style="line-height: 1.9; color: #606266; padding-left: 20px">
      <li v-for="f in features" :key="f">{{ f }}</li>
    </ul>

    <el-divider content-position="left">路由契约(后端已就绪)</el-divider>
    <slot name="api" />
  </el-card>
</template>
