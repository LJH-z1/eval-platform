<script setup>
/**
 * 通用 CRUD 占位页 — 任何模块的列表 + 表单场景
 * <p>
 * 提供统一骨架(查询栏 / 表格 / 新建按钮 / 弹窗),由各负责人填充字段
 */
import { ref, reactive } from 'vue'
import ModulePlaceholder from '@/components/ModulePlaceholder.vue'

defineProps({
  module:   { type: String, required: true },
  owner:    { type: String, required: true },
  title:    { type: String, required: true },
  features: { type: Array,  default: () => [] }
})

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '' })
const dialog = reactive({ visible: false, form: {} })

function load() { /* TODO 调用后端 API 加载 */ }
function onCreate() { dialog.visible = true; dialog.form = {} }
function onSubmit() { /* TODO 提交 */ }
</script>

<template>
  <ModulePlaceholder
    :module="module" :owner="owner" :title="title" :features="features"
  >
    <template #api>
      <p>GET&nbsp;&nbsp;&nbsp; <code>/api/{module}/list</code></p>
      <p>POST&nbsp;&nbsp; <code>/api/{module}</code></p>
      <p>PUT&nbsp;&nbsp;&nbsp; <code>/api/{module}/{id}</code></p>
      <p>DELETE <code>/api/{module}/{id}</code></p>
    </template>
  </ModulePlaceholder>
</template>
