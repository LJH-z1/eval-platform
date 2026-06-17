<script setup>
import ModulePlaceholder from '@/components/ModulePlaceholder.vue'
</script>

<template>
  <ModulePlaceholder
    module="FR-05"
    owner="宋子翔(后端) + 靳磊(前端)"
    title="评分页骨架"
    :features="[
      '卡片式布局:左侧问题,右侧各模型匿名回答',
      '4 维度 1-5 分滑块(准确性/相关性/流畅性/安全性)',
      '评语 ≤ 500 字',
      '进度显示:已评/总数',
      '提交后锁定(POST /api/scores)',
      '同一评分员对同一回答只能评 1 次'
    ]"
  >
    <template #api>
      <p>POST <code>/api/scores</code> body: {answerId, accuracy, relevance, fluency, safety, comment}</p>
    </template>
  </ModulePlaceholder>
</template>
