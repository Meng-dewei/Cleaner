import { defineConfig } from 'vite'
import uni from "@dcloudio/vite-plugin-uni";

// 添加明显的日志，确认配置被加载

// 创建配置对象
const config = {
  plugins: [
    uni()
  ],
  server: {
    port: 9080,
    proxy: {
      '/api': {
        target: 'http://192.168.102.123:11500',
        changeOrigin: true,
		rewrite: (path) => path.replace(/^\/api/, '')
      }
    }
  }
};


export default defineConfig(config);
