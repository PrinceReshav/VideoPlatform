#!/bin/bash
# MinIO bucket initialization script
# Runs after MinIO is healthy to create buckets and set policies

set -e

MINIO_ENDPOINT="http://minio:9000"
MINIO_ACCESS_KEY="${MINIO_ROOT_USER:-minioadmin}"
MINIO_SECRET_KEY="${MINIO_ROOT_PASSWORD:-minioadmin123}"

echo "Waiting for MinIO to be ready..."
until mc alias set local "${MINIO_ENDPOINT}" "${MINIO_ACCESS_KEY}" "${MINIO_SECRET_KEY}" 2>/dev/null; do
  echo "MinIO not ready yet, waiting..."
  sleep 2
done

echo "MinIO is ready. Creating buckets..."

# Create buckets
buckets=("videos" "thumbnails" "hls" "avatars" "banners")
for bucket in "${buckets[@]}"; do
  if mc ls "local/${bucket}" >/dev/null 2>&1; then
    echo "Bucket '${bucket}' already exists"
  else
    mc mb "local/${bucket}"
    echo "Created bucket: ${bucket}"
  fi
done

# Set bucket policies
# Public read for avatars and banners
mc anonymous set public local/avatars
mc anonymous set public local/banners

# Download-only for videos, thumbnails, hls (requires presigned URLs for private content)
mc anonymous set download local/videos
mc anonymous set download local/thumbnails
mc anonymous set download local/hls

# Set bucket lifecycle rules for temporary upload cleanup (optional)
# mc ilm rule add local/videos --expire-days 7 --prefix "uploads/temp/"

# Verify bucket configuration
echo ""
echo "=== Bucket Configuration ==="
for bucket in "${buckets[@]}"; do
  echo "--- ${bucket} ---"
  mc ls "local/${bucket}"
  mc anonymous get "local/${bucket}"
done

echo ""
echo "MinIO initialization complete!"

# Create sample folder structure for HLS
mc mkdir -p local/hls/transcoding-temp 2>/dev/null || true

echo "Done."