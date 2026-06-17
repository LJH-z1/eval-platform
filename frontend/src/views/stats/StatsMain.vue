<script setup>
import ModulePlaceholder from '@/components/ModulePlaceholder.vue'
</script>

<template>
  <ModulePlaceholder
    module="FR-06"
    owner="宋子翔(后端) + 靳磊(前端)"
    title="一致性分析骨架"
    :features="[
      'Fleiss Kappa 系数展示(数字 + 解读文案)',
      '争议项列表(标准差 > 1.5)',
      '评分员排行(平均分/覆盖率/被申诉次数)',
      '模型排名(各维度平均分 + 加权总分)',
      'ECharts 可视化'
    ]"
  >
    <template #api>
      <p>GET <code>/api/stats/kappa?evaluationId=1</code></p>
      <p>GET <code>/api/stats/controversial?evaluationId=1</code></p>
      <p>GET <code>/api/stats/scorer-ranking?evaluationId=1</code></p>
      <p>GET <code>/api/stats/model-ranking?evaluationId=1</code></p>
    </template>
  </ModulePlaceholder>
</template>
