<script setup>
import { RouterView } from 'vue-router'
</script>

<template>
  <el-config-provider>
    <router-view />
  </el-config-provider>
</template>

<style>
#app { min-height: 100vh; }
</style>
