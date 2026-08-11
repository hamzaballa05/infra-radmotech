#!/bin/bash
set -euo pipefail

cd /opt/infra
git pull origin main
cd docker
docker compose up -d --build