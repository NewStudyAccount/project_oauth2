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
      },
      '/login': {
        target: 'http://auth.local:9000',
        changeOrigin: true
      },
      '/logout': {
        target: 'http://auth.local:9000',
        changeOrigin: true
      },
      '/css': {
        target: 'http://auth.local:9000',
        changeOrigin: true
      },
      '/js': {
        target: 'http://auth.local:9000',
        changeOrigin: true
      },
      '/images': {
        target: 'http://auth.local:9000',
        changeOrigin: true
      },
      '/register': {
        target: 'http://auth.local:9000',
        changeOrigin: true
      },
      '/send-code': {
        target: 'http://auth.local:9000',
        changeOrigin: true
      },
      '/consent': {
        target: 'http://auth.local:9000',
        changeOrigin: true
      }
    }
  }
})