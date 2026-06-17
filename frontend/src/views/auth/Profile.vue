<script setup>
/**
 * 个人中心 — 骨架
 * <p>
 * 由【靳磊】实现
 */
import ModulePlaceholder from '@/components/ModulePlaceholder.vue'
</script>

<template>
  <ModulePlaceholder
    module="FR-01"
    owner="靳磊"
    title="个人中心骨架"
    :features="[
      '展示当前用户信息(GET /api/auth/me)',
      '修改密码(POST /api/auth/change-password,oldPwd/newPwd)',
      '退出登录(POST /api/auth/logout)'
    ]"
  />
</template>
