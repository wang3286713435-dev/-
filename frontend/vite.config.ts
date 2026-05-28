import { defineConfig, loadEnv } from 'vite';
import vue from '@vitejs/plugin-vue';
import react from '@vitejs/plugin-react';
import { fileURLToPath, URL } from 'node:url';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const apiTarget = env.VITE_API_TARGET || env.VITE_API_PROXY_TARGET || 'http://127.0.0.1:18080';
  const frontendPort = Number(env.VITE_DEV_PORT || env.VITE_FRONTEND_PORT || 5174);
  const backendEnabled = env.VITE_C_TOWER_BACKEND_ENABLED === 'true'
    || env.VITE_BIM_SUBMISSION_BACKEND === 'true';
  const apiProxy = backendEnabled
    ? {
        '/api': {
          target: apiTarget,
          changeOrigin: true
        }
      }
    : undefined;

  return {
    plugins: [vue(), react()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },
    server: {
      port: Number.isFinite(frontendPort) ? frontendPort : 5174,
      strictPort: true,
      ...(apiProxy ? { proxy: apiProxy } : {})
    }
  };
});
