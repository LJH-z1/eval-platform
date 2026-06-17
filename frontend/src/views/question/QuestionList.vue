<script setup>
import CrudPlaceholder from '@/components/CrudPlaceholder.vue'
</script>

<template>
  <CrudPlaceholder
    module="FR-03"
    owner="向锏楠(后端) + 靳磊(前端)"
    title="问题管理骨架"
    :features="[
      '单题输入(content ≤ 4000 字)',
      '批量导入(Excel/CSV,≤ 200 题/次)',
      '题库管理(公共题库 + 个人题库)',
      '按 category / type 筛选',
      '软删除'
    ]"
  />
</template>
