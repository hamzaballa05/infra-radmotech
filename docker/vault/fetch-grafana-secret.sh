#!/bin/sh
set -eu

VAULT_TOKEN=$(cat /run/secrets/vault_grafana_token)

GF_SECURITY_ADMIN_PASSWORD=""
i=1
while [ "$i" -le 10 ]; do
  RESPONSE=$(wget -qO- --header="X-Vault-Token: $VAULT_TOKEN" http://vault:8200/v1/acadconf/data/grafana_password 2>/dev/null) || true
  GF_SECURITY_ADMIN_PASSWORD=$(echo "$RESPONSE" | grep -o '"value":"[^"]*"' | head -1 | sed 's/"value":"\(.*\)"/\1/')
  [ -n "$GF_SECURITY_ADMIN_PASSWORD" ] && break
  echo "Vault pas encore pret, tentative $i/10..." >&2
  sleep 2
  i=$((i + 1))
done

if [ -z "$GF_SECURITY_ADMIN_PASSWORD" ]; then
  echo "ERREUR : impossible de recuperer le mot de passe Grafana depuis Vault apres 10 tentatives" >&2
  exit 1
fi

export GF_SECURITY_ADMIN_PASSWORD

exec /run.sh
