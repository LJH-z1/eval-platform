<script setup>
/**
 * 模型配置管理 — 骨架
 * <p>
 * 由【向锏楠(后端)+ 靳磊(前端)】实现
 */
import CrudPlaceholder from '@/components/CrudPlaceholder.vue'
</script>

<template>
  <CrudPlaceholder
    module="FR-02"
    owner="向锏楠(后端) + 靳磊(前端)"
    title="模型配置管理骨架"
    :features="[
      '分页查询模型(GET /api/models?provider=M3)',
      '+ 新建模型:填写 name / provider(M3/ZHIPU/QWEN/WENXIN/KIMI)/apiKey/endpoint/temperature/topP/maxTokens',
      '启用/停用切换',
      '删除(被引用的模型不能删)',
      '连接测试:输入测试问题,调用 /api/models/test'
    ]"
  />
</template>
