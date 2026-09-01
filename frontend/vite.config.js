import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 开发环境：所有 API 请求由 Vite 代理转发到网关 8080，同源无跨域，网关的 globalcors 可不配。
// 部署时若前后端不同域，仍需要网关 CORS。
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 3000,
    proxy: {
      '/user': 'http://localhost:8080',
      '/shop': 'http://localhost:8080',
      '/shop-type': 'http://localhost:8080',
      '/shop-review': 'http://localhost:8080',
      '/upload': 'http://localhost:8080',
      '/voucher': 'http://localhost:8080',
      '/voucher-order': 'http://localhost:8080',
      '/seckill': 'http://localhost:8080',
      '/blog': 'http://localhost:8080',
      '/blog-comments': 'http://localhost:8080',
      '/follow': 'http://localhost:8080',
      '/merchant': 'http://localhost:8080'
    }
  }
})
