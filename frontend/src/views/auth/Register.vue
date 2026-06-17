<script setup>
/**
 * 注册页 — 骨架
 * <p>
 * 由【靳磊】实现,后端 POST /api/auth/register(需 ADMIN 权限)
 */
import ModulePlaceholder from '@/components/ModulePlaceholder.vue'
</script>

<template>
  <div class="login-bg">
    <el-card class="login-card" shadow="always">
      <div class="login-title">注册新账号</div>
      <ModulePlaceholder
        module="FR-01"
        owner="刘家豪(后端) + 靳磊(前端)"
        title="注册页骨架"
        :features="[
          '用户名(3-20 位)/密码(6-20,字母+数字)/邮箱(可选)/角色(SCORER/ORGANIZER/VISITOR)',
          '调用 POST /api/auth/register(需要 ADMIN 登录后)',
          '成功后跳转登录页',
          '建议:让管理员在用户管理页直接创建,本页可作为公开申请入口'
        ]"
      />
    </el-card>
  </div>
</template>
