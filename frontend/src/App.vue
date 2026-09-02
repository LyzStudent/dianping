<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, auth } from './api'

const route = useRoute()
const router = useRouter()

onMounted(() => auth.refreshMe())

// 后台（商家/管理员）使用各自 Layout，不套移动端壳
const isBackend = computed(() => route.path.startsWith('/merchant') || route.path.startsWith('/admin'))
// 登录 / 403 页不显示底部 TabBar
const hideTab = computed(() => route.path === '/login' || route.path === '/forbidden')

const roleText = computed(() => {
  switch (auth.role) {
    case 2: return '商家'
    case 3: return '管理员'
    default: return '用户'
  }
})

async function logout() {
  try { await api.post('/user/logout'); } catch { /* token 失效也无妨 */ }
  auth.clear();
  router.replace('/shops'); // SPA 内跳转，不触发整页刷新
}
</script>

<template>
  <!-- 用户端：移动 App 壳（顶部条 + 内容滚动区 + 底部 TabBar） -->
  <div v-if="!isBackend" class="app-shell">
    <div class="app-top">
      <span class="brand">🐋 蓝鲸点评</span>
      <div class="app-top-right">
        <template v-if="auth.me">
          <span class="role-badge">{{ roleText }}</span>
          <router-link v-if="auth.role === 2" class="app-link" to="/merchant/shop">商家后台</router-link>
          <router-link v-else-if="auth.role === 3" class="app-link" to="/admin/users">管理后台</router-link>
          <button class="app-link" @click="logout">退出</button>
        </template>
        <router-link v-else class="app-link" to="/login">登录</router-link>
      </div>
    </div>

    <div class="app-main">
      <router-view />
    </div>

    <nav v-if="!hideTab" class="tab-bar">
      <router-link class="tab-item" to="/shops"><span class="ico">🏠</span>首页</router-link>
      <router-link class="tab-item" to="/nearby"><span class="ico">🧭</span>附近</router-link>
      <router-link class="tab-item" to="/blog"><span class="ico">✍️</span>笔记</router-link>
      <router-link class="tab-item" to="/me"><span class="ico">👤</span>我的</router-link>
    </nav>
  </div>

  <!-- 商家/管理员后台：由各自 Layout 渲染 -->
  <router-view v-else />
</template>
