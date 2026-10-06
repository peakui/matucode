import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const backendTarget = env.VITE_API_TARGET || 'http://127.0.0.1:8080'

  return {
    plugins: [react()],
    server: {
      proxy: {
        '/api/messages/ws': {
          target: backendTarget,
          changeOrigin: true,
          ws: true,
          rewrite: (path) => path.replace(/^\/api/, ''),
          configure: (proxy) => {
            proxy.on('proxyReqWs', (proxyReq, req) => {
              // Browsers always send Origin on WS handshakes; the gateway CORS filter
              // only allows the prod origin, so the dev handshake gets a 403. Drop it.
              proxyReq.removeHeader('origin')
              console.log('[vite][ws-proxy]', req.url, '->', backendTarget)
            })
            proxy.on('error', (error) => {
              console.error('[vite][ws-proxy:error]', error.message)
            })
          },
        },
        '/api': {
          target: backendTarget,
          changeOrigin: true,
          ws: true,
          rewrite: (path) => path.replace(/^\/api/, ''),
        },
      },
    },
  }
})
