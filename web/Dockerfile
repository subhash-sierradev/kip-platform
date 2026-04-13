# ============================================================
# Stage 1: Build — compile Vue.js + TypeScript into static assets
# ============================================================
FROM node:20-alpine AS build

WORKDIR /app

# Copy dependency manifests first for layer caching
COPY web/package.json web/package-lock.json ./

# Install dependencies (ci = clean install, reproducible)
RUN npm ci --ignore-scripts

# Copy source and build
COPY web/ .
RUN npm run build:docker

# ============================================================
# Stage 2: Serve — lightweight Nginx to serve the SPA
# ============================================================
FROM nginx:1.27-alpine AS production

# Remove default nginx config and static files
RUN rm -rf /etc/nginx/conf.d/default.conf /usr/share/nginx/html/*

# Copy built assets from build stage
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget -qO- http://127.0.0.1/ || exit 1
