import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), 'VITE_');

  return {
    plugins: [react()],
    server: {
      port: 5173,
      strictPort: true,
      proxy: {
        // Configurable per developer; defaults to the local backend port.
        '/api': env.VITE_API_PROXY_TARGET || 'http://localhost:8080',
      },
    },
  };
});
