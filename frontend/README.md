# 蓝鲸点评 · 前端（Vue3 + Vite）

「蓝鲸点评」微服务项目的前端，移动端壳布局（430px 居中 + 底部 TabBar），覆盖**用户端 / 商家端 / 管理员端**三套界面。

## 技术栈

- Vue 3（`<script setup>` 组合式 API）
- vue-router 4（路由守卫按角色跳转、未登录重定向）
- Vite 6（开发服务器代理 → 网关 8080，同源免 CORS）

## 目录结构

```
frontend/
├── index.html
├── vite.config.js      # 代理 /user /shop /shop-type /shop-review /shop/favorite /upload /banner /voucher /seckill /voucher-order /blog /blog-comments /follow -> 8080
├── package.json
└── src/
    ├── main.js
    ├── App.vue           # 顶部导航 + 登录态 + 底部 TabBar
    ├── router.js         # 路由 + 角色守卫（public / roles: [2] 商家 / roles: [3] 管理员）
    ├── api.js            # fetch 封装 + 响应式登录态 store（auth）
    ├── style.css         # 全局样式 + 品牌变量（--brand）
    ├── components/
    │   ├── StarRating.vue    # 星级展示（支持半星）
    │   └── ShopListItem.vue  # 店铺卡片（首页/附近/收藏复用，含 ES 高亮防 XSS）
    └── views/
        ├── ShopsView.vue       # 用户端首页：轮播/金刚区/热词/搜索/详情/评价/券
        ├── NearbyView.vue      # 附近（GEO 距离）
        ├── BlogView.vue        # 笔记流：热门/关注/我的/发布/点赞/评论/关注
        ├── BlogDetailView.vue  # 笔记详情
        ├── SeckillView.vue     # 秒杀/券列表/下单
        ├── MeView.vue          # 我的：会员卡/签到/菜单
        ├── MyFavoritesView.vue # 我的收藏
        ├── MyOrdersView.vue    # 我的订单
        ├── LoginView.vue       # 登录/注册（验证码登录、密码登录、三端角色注册）
        ├── ForbiddenView.vue   # 403
        ├── MerchantLayout.vue  # 商家端布局（角色2）
        ├── MerchantShopView.vue / MerchantVoucherView.vue / MerchantOrdersView.vue
        ├── AdminLayout.vue     # 管理员端布局（角色3）
        └── AdminUsersView.vue / AdminShopsView.vue / AdminStatsView.vue
```

## 运行

```bash
cd frontend
npm install          # 首次
npm run dev          # http://localhost:3000，API 自动代理到网关 8080
```

## 登录与三端角色

- **注册**：手机号 + 密码（可选邮箱），注册时选择角色：用户 / 商家 / 管理员
- **登录**：验证码登录（手机号 / 邮箱）或密码登录
- **路由守卫**：`/merchant/*` 仅角色 2 可进，`/admin/*` 仅角色 3 可进，越权跳 `/forbidden`
- **演示账号**：见后端 `sql/dianping_user.sql` 中 `points/role` 的 INSERT（含商家 999002、管理员等）

## 使用流程

1. **首页**：热词点击搜索 / 轮播切换 / 金刚区分类 / 排序 chips / 搜索（ES 高亮）；进详情看电话、评价、券
2. **附近**：浏览器定位或默认坐标 → 按距离展示附近店铺
3. **笔记**：发布探店笔记（需登录）→ 点赞 / 评论 / 关注作者；「关注」页看关注动态
4. **秒杀**：创建秒杀券 → 列表 → 「去秒杀」，验证 Lua 预扣 + MQ 异步 + 一人一单
5. **我的**：签到（Redis 位图）、会员卡（积分/等级）、收藏、订单

## 注意事项

- **秒杀券判定**：后端列表 SQL `left join` 秒杀表，只有秒杀券 `stock` 非空，前端以此判断
- **Token**：存 `localStorage('dp_token')`，请求头 `Authorization` 传原始 JWT（无 Bearer 前缀）
- **文本里的 `<br>`**：笔记/评价数据含 `<br>` 换行标签，前端统一 `fmtContent` 转 `\n` + `white-space: pre-line` 渲染，防字面显示
- **图片**：上传文件落本地磁盘，无 HTTP 静态映射，浏览器预览不到（灰块正常），不影响功能
