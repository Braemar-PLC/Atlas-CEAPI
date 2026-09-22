import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react-swc'
import { tanstackRouter } from '@tanstack/router-plugin/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    tanstackRouter({
      target: 'react',
      autoCodeSplitting: true,
      routesDirectory: 'src/routes',
      generatedRouteTree: 'src/routes/routeTree.gen.ts',
    }),
    react(),
  ],
  resolve: {
    alias: {
      "@": new URL("./src", import.meta.url).pathname,
    },
  },
  server: {
    proxy: {
      // Forward /api/* to the Atlas API so the browser sees one origin (no CORS set up on the API).
      // secure: false accepts the ASP.NET development certificate, which Node does not trust.
      "/api": {
        target: "https://localhost:7001",
        changeOrigin: true,
        secure: false,
      },
    },
  },
})


