import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    host: '0.0.0.0',  // 监听所有接口，允许通过 client.a.local 访问
    port: 5173,
    proxy: {
      '/userinfo': {
        target: 'http://auth.local:9000',
        changeOrigin: true
      }
    }
  }
})
