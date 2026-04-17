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
# Stage 2: Serve — static file server (nginx handled by infrastructure)
# ============================================================
FROM node:20-alpine AS production

WORKDIR /app

# Install serve for static file serving with SPA fallback
RUN npm install -g serve

# Copy built assets from build stage
COPY --from=build /app/dist ./dist

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget -qO- http://127.0.0.1/ || exit 1

# -s enables SPA routing fallback (all routes → index.html)
CMD ["serve", "-s", "dist", "-l", "80"]
