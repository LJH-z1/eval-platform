<script setup>
/**
 * 评测主页面 — 骨架
 * <p>
 * 由【梁倩倩(后端)+ 靳磊(前端)】实现
 */
import ModulePlaceholder from '@/components/ModulePlaceholder.vue'
</script>

<template>
  <ModulePlaceholder
    module="FR-04"
    owner="梁倩倩(后端) + 靳磊(前端)"
    title="评测主页面骨架"
    :features="[
      '创建评测:选模型(≥2) + 选问题(≤50)',
      '启动评测 → 显示进度条',
      '并排展示各模型回答(对齐 §3.4.04)',
      '差异高亮(前端用 diff 库,绿/红)',
      '流式输出:EventSource 订阅 /api/evaluations/{id}/stream',
      '匿名化:评分展示阶段模型名 Model A/B/C/D'
    ]"
  >
    <template #api>
      <p>POST <code>/api/evaluations</code> body: {name, modelIds, questionIds}</p>
      <p>POST <code>/api/evaluations/{id}/run</code></p>
      <p>GET  <code>/api/evaluations/{id}/answers</code></p>
      <p>GET  <code>/api/evaluations/{id}/stream</code> (SSE)</p>
    </template>
  </ModulePlaceholder>
</template>
