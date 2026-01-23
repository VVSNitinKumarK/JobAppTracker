# ---------- 1) Build frontend ----------
FROM node:22-alpine AS frontend-build
WORKDIR /app

# Build arg for API URL (baked into frontend at build time)
ARG VITE_API_BASE_URL=/api
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}

RUN corepack enable

# Copy only the dependency manifests first (cache-friendly)
COPY frontend/package.json frontend/pnpm-lock.yaml ./frontend/

WORKDIR /app/frontend
RUN pnpm install --frozen-lockfile

# Now copy the rest of the frontend
COPY frontend/. .

# Build
RUN pnpm run build


# ---------- 2) Nginx runtime ----------
FROM nginx:alpine

COPY deploy/nginx/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=frontend-build /app/frontend/dist /usr/share/nginx/html

EXPOSE 80