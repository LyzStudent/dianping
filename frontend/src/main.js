import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { setForbiddenHandler } from './api'
import './style.css'

// 后端返回 403（角色不匹配）时跳转到提示页，由 api.js 统一触发
setForbiddenHandler(() => router.push('/forbidden'))

createApp(App).use(router).mount('#app')
