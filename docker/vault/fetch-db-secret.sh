#!/bin/bash
set -euo pipefail

VAULT_TOKEN=$(cat /run/secrets/vault_app_token)

MYSQL_ROOT_PASSWORD=""
for i in $(seq 1 10); do
  RESPONSE=$(curl -s --header "X-Vault-Token: $VAULT_TOKEN" http://vault:8200/v1/acadconf/data/db_password) || true
  MYSQL_ROOT_PASSWORD=$(echo "$RESPONSE" | grep -o '"value":"[^"]*"' | head -1 | sed 's/"value":"\(.*\)"/\1/')
  [ -n "$MYSQL_ROOT_PASSWORD" ] && break
  echo "Vault pas encore prêt, tentative $i/10..." >&2
  sleep 2
done

if [ -z "$MYSQL_ROOT_PASSWORD" ]; then
  echo "ERREUR : impossible de récupérer le mot de passe depuis Vault après 10 tentatives" >&2
  echo "Dernière réponse brute : $RESPONSE" >&2
  exit 1
fi

export MYSQL_ROOT_PASSWORD

exec docker-entrypoint.sh mysqld
