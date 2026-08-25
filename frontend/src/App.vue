<script setup>
import { onMounted } from 'vue'
import { api, auth } from './api'

onMounted(() => auth.refreshMe())

async function logout() {
  try { await api.post('/user/logout'); } catch { /* token 失效也无妨 */ }
  auth.clear();
}
</script>

<template>
  <header class="topbar">
    <div class="brand">🦘 大众点评 · 微服务演示</div>
    <div class="topbar-right">
      <template v-if="auth.me">
        <span>{{ auth.me.nickName }}</span>
        <button class="logout-btn" @click="logout">退出登录</button>
      </template>
      <span v-else class="muted">未登录</span>
    </div>
  </header>

  <nav class="tabs">
    <router-link class="tab-btn" to="/shops">店铺</router-link>
    <router-link class="tab-btn" to="/seckill">秒杀</router-link>
    <router-link class="tab-btn" to="/blog">博客</router-link>
    <router-link class="tab-btn" to="/me">我的</router-link>
  </nav>

  <main class="container">
    <router-view />
  </main>
</template>
