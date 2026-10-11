#!/usr/bin/env bash
set -eu

ENDPOINT="http://minio:9000"
BUCKET="banner"

echo "Waiting for MinIO..."

until aws --endpoint-url "$ENDPOINT" s3api list-buckets >/dev/null 2>&1; do
  sleep 2
done

echo "MinIO is ready"

if ! aws --endpoint-url "$ENDPOINT" s3api head-bucket \
  --bucket "$BUCKET" >/dev/null 2>&1; then
  echo "Creating bucket: $BUCKET"
  aws --endpoint-url "$ENDPOINT" s3api create-bucket \
    --bucket "$BUCKET"
else
  echo "Bucket already exists: $BUCKET"
fi

echo "Applying policy..."

aws --endpoint-url "$ENDPOINT" s3api put-bucket-policy \
  --bucket "$BUCKET" \
  --policy file:///aws/policy.json

echo "Bucket initialized successfully"
