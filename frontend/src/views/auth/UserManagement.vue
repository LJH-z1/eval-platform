<script setup>
/**
 * 用户管理 — 骨架(仅 ADMIN 可见)
 * <p>
 * 由【靳磊】实现
 */
import CrudPlaceholder from '@/components/CrudPlaceholder.vue'
</script>

<template>
  <CrudPlaceholder
    module="FR-01"
    owner="刘家豪(后端) + 靳磊(前端)"
    title="用户管理骨架"
    :features="[
      '分页查询用户(GET /api/users?pageNum=&pageSize=&role=)',
      '+ 新建用户(POST /api/auth/register,需要当前用户是 ADMIN)',
      '禁用用户(POST /api/users/{id}/disable)',
      '角色过滤(ADMIN/ORGANIZER/SCORER/VISITOR)'
    ]"
  >
    <template #api>
      <p>GET&nbsp;&nbsp;&nbsp; <code>/api/users?pageNum=1&amp;pageSize=10&amp;role=SCORER</code></p>
      <p>POST&nbsp;&nbsp; <code>/api/auth/register</code></p>
      <p>POST&nbsp;&nbsp; <code>/api/users/{id}/disable</code></p>
    </template>
  </CrudPlaceholder>
</template>
