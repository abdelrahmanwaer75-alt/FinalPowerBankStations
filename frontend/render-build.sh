#!/usr/bin/env bash
set -euo pipefail

if [[ -z "${API_BASE_URL:-}" ]]; then
  echo "API_BASE_URL must point to the deployed backend URL." >&2
  exit 1
fi
if [[ ! "$API_BASE_URL" =~ ^https://[A-Za-z0-9.-]+(:[0-9]+)?/?$ ]]; then
  echo "API_BASE_URL must be an HTTPS origin, for example https://powerbank-api.onrender.com" >&2
  exit 1
fi

api_origin="${API_BASE_URL%/}"
printf "window.APP_CONFIG = { API_BASE_URL: '%s/api' };\n" "$api_origin" > frontend/js/config.js
