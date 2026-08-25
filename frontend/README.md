# 大众点评 · 微服务演示前端（Vue3 + Vite）

单页应用，用于**展示 + 测试**后端 6 个微服务（网关 / user / shop / blog / trade）。

## 技术栈

- Vue 3（`<script setup>` 组合式 API）
- vue-router 4（店铺 / 秒杀 / 博客 / 我的 四个路由）
- Vite 6（开发服务器自带代理 → 网关 8080，**同源免 CORS**）

## 目录结构

```
frontend/
├── index.html
├── vite.config.js        # 代理：/user /shop /shop-type /shop-review /upload /voucher /seckill /blog /blog-comments /follow -> 8080
├── package.json
└── src/
    ├── main.js
    ├── App.vue           # 顶栏 + 登录态 + 路由导航
    ├── router.js
    ├── api.js            # fetch 封装 + 响应式登录态 store（auth）
    ├── style.css
    └── views/
        ├── ShopsView.vue   # 店铺类型/列表/ES搜索/详情/评价/券
        ├── SeckillView.vue # 普通券/秒杀券创建、券列表、秒杀下单
        ├── BlogView.vue    # 发笔记/热门/我的/关注动态/点赞/评论/关注
        └── MeView.vue      # 登录/签到/用户信息
```

## 运行

```bash
cd frontend
npm install          # 首次
npm run dev          # http://localhost:3000，API 自动代理到网关 8080
```

> 开发模式走 Vite 代理，同源，**不需要**网关 CORS。若以后把构建产物 `dist/` 单独部署到别的域，才需要网关的 `globalcors`（已配好）。

## 使用流程建议

1. **我的 → 登录**：填邮箱 →「获取验证码」，验证码打印在 **user-service 控制台日志**（MockMailSender），填入后登录。
2. **我的 → 签到**：验证 Redis 位图签到 + 连续签到天数。
3. **店铺**：分类切换 / ES 搜索（高亮）/ 店铺详情 → 评价（登录后可发表/更新）、店铺券列表。
4. **秒杀**：创建秒杀券（时间默认=现在~明天）→ 查询券列表 → 登录后「去秒杀」，验证 Redis 预扣 + 一人一单 + Seata 全局事务。
5. **博客**：发笔记（需登录）→ 点赞 / 评论 / 点赞评论 / 关注作者；「我的笔记」「关注动态」。

## 注意事项

- **秒杀券判定**：后端列表 SQL `left join` 秒杀表，只有秒杀券 `stock` 非空，前端以此判断（`type` 后端创建时没置 1）。
- **创建券必须传 `status:1`**：列表 SQL 带 `AND v.status=1`，不传列表里看不到。
- **图片**：上传文件落本地磁盘，无 HTTP 静态映射，浏览器预览不到（灰块正常），不影响功能。
- **Token**：存 `localStorage('dp_token')`，请求头 `Authorization` 传原始 JWT（无 Bearer 前缀）。
- **秒杀下单 `/seckill/{id}` 需登录**；Sentinel 限流时返回「系统繁忙，请稍后再试」。

## 其他
- 本地 `application.yml` 是配置主源，Nacos 只放 `common.yml` + 各服务 datasource 覆盖。
