import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { fileURLToPath, URL } from 'node:url';

const apiTarget = process.env.VITE_API_TARGET ?? 'http://127.0.0.1:18080';
const devPort = Number(process.env.VITE_DEV_PORT ?? 5174);
const backendEnabled = process.env.VITE_C_TOWER_BACKEND_ENABLED === 'true'
  || process.env.VITE_BIM_SUBMISSION_BACKEND === 'true';
const apiProxy = backendEnabled
  ? {
      '/api': {
        target: apiTarget,
        changeOrigin: true
      }
    }
  : undefined;

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: devPort,
    strictPort: true,
    ...(apiProxy ? { proxy: apiProxy } : {})
  }
});
