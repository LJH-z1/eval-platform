<script setup>
import ModulePlaceholder from '@/components/ModulePlaceholder.vue'
</script>

<template>
  <ModulePlaceholder
    module="FR-08"
    owner="周文泽(后端) + 靳磊(前端)"
    title="报告导出骨架"
    :features="[
      '导出 Excel 按钮(GET /api/export/{id}/excel)',
      '导出 PDF 按钮(GET /api/export/{id}/pdf)',
      '文件名格式:评测报告_{名称}_{YYYYMMDD}.xlsx',
      '报告内容:封面/概述/调用统计/评分统计/Kappa/详细结果/结论'
    ]"
  />
</template>
