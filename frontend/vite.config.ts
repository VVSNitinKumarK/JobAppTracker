import path from "path";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

export default defineConfig({
    plugins: [react()],
    resolve: {
        alias: {
            "@": path.resolve(__dirname, "./src"),
        },
    },
    publicDir: "public", // Assets in public/ will be copied to build root
    build: {
        rollupOptions: {
            input: {
                // Add your entry points here
                // For example:
                // contentScript: path.resolve(__dirname, "src/contentScript.ts"),
                // background: path.resolve(__dirname, "src/background.ts"),
                // popup: path.resolve(__dirname, "index.html"),
            },
        },
    },
});
