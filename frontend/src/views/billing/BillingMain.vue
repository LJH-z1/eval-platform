<script setup>
import ModulePlaceholder from '@/components/ModulePlaceholder.vue'
</script>

<template>
  <ModulePlaceholder
    module="FR-07"
    owner="梁倩倩(后端) + 靳磊(前端)"
    title="成本统计骨架"
    :features="[
      '汇总卡片:总调用 / 总 token / 总费用 / 平均耗时',
      '折线图:按时段展示调用量与费用(ECharts)',
      '柱状图:各模型对比',
      'CSV 导出按钮'
    ]"
  >
    <template #api>
      <p>GET <code>/api/billing/summary?evaluationId=1</code></p>
      <p>GET <code>/api/billing/time-series?evaluationId=1&amp;granularity=hour</code></p>
      <p>GET <code>/api/billing/by-model?evaluationId=1</code></p>
      <p>GET <code>/api/billing/export?evaluationId=1</code></p>
    </template>
  </ModulePlaceholder>
</template>
