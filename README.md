蓝鲸点评（dianping）

仿大众点评的本地生活服务平台，微服务架构 + 三端（用户 / 商家 / 管理员），完整实现从店铺浏览、笔记社区到秒杀下单的业务闭环。

技术栈

| 层面 | 技术 |
|---|---|
| 微服务 | Spring Cloud Alibaba 2023.0.1、Nacos（注册 + 配置）、Spring Cloud Gateway、OpenFeign + LoadBalancer |
| 框架 | Spring Boot 3.2.12、Java 17、MyBatis-Plus 3.5.10 |
| 存储 | MySQL（每服务独立分库）、Redis、Elasticsearch 8（IK 中文分词） |
| 中间件 | RabbitMQ、Seata（AT 分布式事务）、Sentinel（限流）、Redisson |
| 认证 | JWT（无状态 + 网关校验 + 黑名单登出） |
| 前端 | Vue3 `<script setup>` + Vite 6 + vue-router 4（移动端壳布局） |

模块与端口

gateway         8080   网关：路由、JWT 校验、traceId、白名单/保护路径、Sentinel 限流
user-service    8081   用户：验证码/密码登录注册、验证码限流、签到位图、会员等级、封禁
shop-service    8082   店铺：Redis 缓存、GEO 附近、ES 搜索、Banner、热词、收藏、审核状态机
blog-service    8083   笔记：发笔记、点赞 ZSet、关注推流、评论
trade-service   8084   秒杀：Lua 扣库存、MQ 异步下单、Seata 事务、订单状态机
common          -      公共：统一返回、异常处理、JWT、拦截器、traceId、Feign Client

核心设计

1. 秒杀闭环（trade-service）
Redis Lua 原子扣库存 + 一人一单**（`seckill.lua`：`seckill:stock:*` 扣库存、`seckill:order:*` Set 判重）
MQ 异步下单：Lua 通过后发消息到 RabbitMQ，消费者 `@GlobalTransactional` 兜底 —— 一人一单（状态机计数 `status in(1,2,3)`）、乐观扣库存、写订单、跨服务加积分（失败抛异常整单回滚）
关单补偿：定时任务 Redisson 分布式锁（多实例只跑一个）分页扫描超时未支付订单，关单释放库存，支持"关单后重新抢购"
订单状态机：未支付 → 已支付 → 已核销 / 已关闭，支付、核销均用乐观锁（`where status=?`）保证并发幂等
全局唯一订单 ID：自研 `RedisIdWorker`（时间戳 + 序列号）

2. 店铺缓存与搜索（shop-service）
缓存穿透：查无数据写空值 + 短 TTL
缓存击穿：互斥锁（`setIfAbsent` + 自旋重试）+ 逻辑过期 + 线程池异步重建
双写一致：先更新 DB 再删缓存
Redis GEO：附近店铺按距离排序 + 分页
ES 搜索：IK 分词、`MultiMatch` 多字段加权（name^3）、高亮回填、回库过滤上架状态；店铺保存/下架时增量同步（`indexById`/`deleteById`），并提供全量重建 `syncAll`
店铺审核状态机：商家新建 → 待审核(0) → 管理员上架(1)/下架(2)，商家不可自改状态

3. 用户体系（user-service）
手机号 / 邮箱验证码登录、密码登录、注册（手机号 + 密码），无账号验证码登录自动建号
验证码频率限流：1 分钟 1 次 + 5 分钟 5 次 → 一级限制，累计超限 → 二级限制（Redis ZSet 计数 + Set 限位）
签到：Redis BitMap `setBit` 签到、`bitField` 连续签到统计、整月日期解析（高位在前处理）
会员等级：积分 → 普卡 / 银卡 / 金卡 / 黑金；秒杀成功跨服务加积分
封禁：`role<=0` 判封，验证码 / 密码登录入口一致校验；管理员可解封
JWT 黑名单登出：登出 token 写 Redis 黑名单，网关命中即拒绝

4. 笔记社区（blog-service）
点赞：Redis ZSet（score=时间戳）记录点赞用户 → 展示最早点赞 Top5 + 是否已赞
关注推流：发笔记 fan-out 写入每个粉丝的收件箱 ZSet
关注动态滚动分页：`reverseRangeByScoreWithScores` + `minTime/offset` 处理同分页边界
跨服务 Feign 查作者信息 / 点赞列表

5. 网关与可观测（gateway）
`AuthGlobalFilter`：JWT 校验、`excludepaths` 公开白名单、`protectpaths` 保护后台路径（防 `/shop/**` 白名单漏检 `/shop/admin/**`）
traceId 全链路透传：网关生成 → `TraceIdGlobalFilter` / `TraceIdInterceptor` 向下游传递，日志贯穿请求

6. 三端（Vue3 + Vue Router 守卫）
用户端：首页（轮播 / 金刚区 / 热词 / 搜索）、附近（GEO）、笔记、秒杀、我的（会员卡 / 签到）
商家端：店铺管理、券管理、订单（后端越权校验：只能操作自己店铺）
管理员端：用户管理（封禁 / 解封）、店铺审核、数据统计
路由守卫按角色跳转 + 未登录重定向；ES 高亮关键词前端防 XSS（只还原 `<em>`）

7. 数据库

```
dianping_user   tb_user / tb_sign
dianping_shop   tb_shop / tb_shop_type / tb_shop_review / tb_user_favorite / tb_banner
dianping_blog   tb_blog / tb_blog_comments / tb_follow
dianping_trade  tb_voucher / tb_seckill_voucher / tb_voucher_order
```
8. 数据库

前置环境：MySQL、Redis、Nacos、Elasticsearch（含 IK 分词插件）、RabbitMQ、Seata、Sentinel Dashboard。

1. 建库：依次执行 `sql/dianping_*.sql`（含建库建表 + 演示数据）
2. ES 索引：启动 shop-service 后调用 `POST /shop/es/sync`（全量重建，首次必需）
3. 启动服务：Nacos 注册中心起来后，按序启动 `gateway → user → shop → blog → trade`（本地 `application.yml` 为配置主源，Nacos 只做覆盖）
4. 前端：
   ```bash
   cd frontend
   npm install
   npm run dev   # http://localhost:3000，Vite 代理到网关 8080
   ```


9. 项目结构

```
dianping/
├── gateway/         网关（路由 / JWT / traceId / Sentinel）
├── user-service/    用户服务
├── shop-service/    店铺服务（缓存 / GEO / ES）
├── blog-service/    笔记服务
├── trade-service/   交易服务（秒杀）
├── common/          公共模块（统一返回 / JWT / 拦截器 / Feign）
├── sql/             各服务分库初始化脚本
├── frontend/        Vue3 三端前端
└── docs/            Apifox 接口文档等
```
