#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 2 ]]; then
  echo "Usage: bash deploy/frontend-aws/deploy-s3.sh <s3-bucket-name> <cloudfront-distribution-id>"
  exit 1
fi

BUCKET_NAME="$1"
DISTRIBUTION_ID="$2"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
FRONT_DIR="${ROOT_DIR}/quickdelivery-googlemaps-front"

cd "${FRONT_DIR}"

if [[ ! -f .env.production ]]; then
  echo ".env.production missing in ${FRONT_DIR}"
  echo "Copy deploy/frontend-aws/.env.production.template to quickdelivery-googlemaps-front/.env.production first."
  exit 1
fi

npm install
npm run build

aws s3 sync dist/ "s3://${BUCKET_NAME}" --delete
aws cloudfront create-invalidation --distribution-id "${DISTRIBUTION_ID}" --paths "/*"

echo "Front deployed to s3://${BUCKET_NAME} and CloudFront invalidation requested."
