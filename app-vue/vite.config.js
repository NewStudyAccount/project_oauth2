import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/userinfo': {
        target: 'http://auth.local:9000',
        changeOrigin: true
      }
    }
  }
})
