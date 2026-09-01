import { createRouter, createWebHistory } from 'vue-router'
import { auth, homeByRole } from './api'

// ---- 用户端 ----
import ShopsView from './views/ShopsView.vue'
import SeckillView from './views/SeckillView.vue'
import BlogView from './views/BlogView.vue'
import MeView from './views/MeView.vue'
import NearbyView from './views/NearbyView.vue'
import MyFavoritesView from './views/MyFavoritesView.vue'
import MyOrdersView from './views/MyOrdersView.vue'

// ---- 通用 ----
import LoginView from './views/LoginView.vue'
import ForbiddenView from './views/ForbiddenView.vue'

// ---- 商家端 ----
import MerchantLayout from './views/MerchantLayout.vue'
import MerchantShopView from './views/MerchantShopView.vue'
import MerchantVoucherView from './views/MerchantVoucherView.vue'
import MerchantOrdersView from './views/MerchantOrdersView.vue'

// ---- 管理员端 ----
import AdminLayout from './views/AdminLayout.vue'
import AdminUsersView from './views/AdminUsersView.vue'
import AdminShopsView from './views/AdminShopsView.vue'
import AdminStatsView from './views/AdminStatsView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    // 通用页（无需登录）
    { path: '/login', component: LoginView, meta: { title: '登录', public: true } },
    { path: '/forbidden', component: ForbiddenView, meta: { title: '无权访问', public: true } },

    // ---- 用户端 ----
    { path: '/', redirect: '/shops' },
    { path: '/shops', component: ShopsView, meta: { title: '首页', public: true } },
    { path: '/nearby', component: NearbyView, meta: { title: '附近', public: true } },
    { path: '/my-favorites', component: MyFavoritesView, meta: { title: '我的收藏' } },
    { path: '/my-orders', component: MyOrdersView, meta: { title: '我的订单' } },
    { path: '/blog', component: BlogView, meta: { title: '笔记', public: true } },
    { path: '/seckill', component: SeckillView, meta: { title: '秒杀' } },
    { path: '/me', component: MeView, meta: { title: '我的' } },

    // ---- 商家端（仅角色2） ----
    {
      path: '/merchant',
      component: MerchantLayout,
      meta: { roles: [2], title: '商家后台' },
      redirect: '/merchant/shop',
      children: [
        { path: 'shop', component: MerchantShopView, meta: { title: '店铺管理' } },
        { path: 'voucher', component: MerchantVoucherView, meta: { title: '优惠券' } },
        { path: 'orders', component: MerchantOrdersView, meta: { title: '订单' } },
      ],
    },

    // ---- 管理员端（仅角色3） ----
    {
      path: '/admin',
      component: AdminLayout,
      meta: { roles: [3], title: '管理员后台' },
      redirect: '/admin/users',
      children: [
        { path: 'users', component: AdminUsersView, meta: { title: '用户管理' } },
        { path: 'shops', component: AdminShopsView, meta: { title: '店铺审核' } },
        { path: 'stats', component: AdminStatsView, meta: { title: '数据统计' } },
      ],
    },

    // 兜底
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

router.beforeEach(async (to) => {
  if (to.meta.title) document.title = to.meta.title + ' · 大众点评'

  // 已登录还去登录页 → 按角色回首页
  if (to.path === '/login' && auth.loggedIn) {
    if (!auth.me) await auth.refreshMe()
    return homeByRole(auth.me ? auth.me.role : 1)
  }

  // 公开页直接放行
  if (to.meta.public) return true

  // 其余页面需要登录
  if (!auth.loggedIn) return { path: '/login', query: { redirect: to.fullPath } }

  // 刷新用户信息（顺便拿角色），失败则登出
  if (!auth.me) {
    await auth.refreshMe()
    if (!auth.loggedIn) return { path: '/login', query: { redirect: to.fullPath } }
  }

  // 角色校验
  if (to.meta.roles && !to.meta.roles.includes(auth.me.role)) return '/forbidden'

  return true
})

export default router
