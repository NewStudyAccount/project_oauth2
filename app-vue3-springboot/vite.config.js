import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    host: 'client.a.local',
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://client.a.local:8082',
        changeOrigin: true
      },
      '/oauth2': {
        target: 'http://client.a.local:8082',
        changeOrigin: true
      },
      '/login': {
        target: 'http://client.a.local:8082',
        changeOrigin: true
      },
      '/logout': {
        target: 'http://client.a.local:8082',
        changeOrigin: true
      }
    }
  }
})
