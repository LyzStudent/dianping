import { createRouter, createWebHistory } from 'vue-router'
import ShopsView from './views/ShopsView.vue'
import SeckillView from './views/SeckillView.vue'
import BlogView from './views/BlogView.vue'
import MeView from './views/MeView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/shops' },
    { path: '/shops', component: ShopsView, meta: { title: '店铺' } },
    { path: '/seckill', component: SeckillView, meta: { title: '秒杀' } },
    { path: '/blog', component: BlogView, meta: { title: '博客' } },
    { path: '/me', component: MeView, meta: { title: '我的' } }
  ]
})

export default router
