import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";
import path from "path";

export default defineConfig({
  plugins: [react()],
  test: {
    environment: "jsdom",
    setupFiles: ["./vitest.setup.ts"],
    globals: true,
    css: false,
    coverage: { provider: "v8" },
  },
  resolve: {
    alias: {
      "@atlas/data": path.resolve(__dirname, "packages/data/src/index.ts"),
      "@atlas/external": path.resolve(__dirname, "packages/external/src/index.ts"),
      "@": path.resolve(__dirname, "./src"),
    },
  },
  
  server: {
    proxy: {
      "/api/natgas/stream": {
        target: "http://localhost:9999",
        changeOrigin: true,
        rewrite: () => "/connect",
      },
    },
  },

});