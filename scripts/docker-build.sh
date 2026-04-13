#!/bin/sh
# ================================================================
# Build all KIP Docker images
# Usage: ./scripts/docker-build.sh [version]
# Example: ./scripts/docker-build.sh 0.157.1
# ================================================================
set -e

VERSION="${1:-latest}"
echo "Building KIP Platform Docker images (tag: $VERSION)"
echo "=================================================="

echo ""
echo "[1/3] Building frontend (kip-web:$VERSION)..."
docker build -f web/Dockerfile -t "kip-web:$VERSION" .

echo ""
echo "[2/3] Building IMS (kip-ims:$VERSION)..."
docker build -f api/integration-management-service/Dockerfile -t "kip-ims:$VERSION" .

echo ""
echo "[3/3] Building IES (kip-ies:$VERSION)..."
docker build -f api/integration-execution-service/Dockerfile -t "kip-ies:$VERSION" .

echo ""
echo "=================================================="
echo "All images built successfully!"
echo ""
docker images --filter "reference=kip-*" --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}\t{{.CreatedSince}}"
