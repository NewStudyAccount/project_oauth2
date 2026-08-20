import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    host: 'auth.local',
    port: 5174,
    proxy: {
      '/api': {
        target: 'http://auth.local:9000',
        changeOrigin: true
      }
    }
  }
})
