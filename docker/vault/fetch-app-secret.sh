#!/bin/sh
set -eu

VAULT_TOKEN=$(cat /run/secrets/vault_app_token)

MYSQLPASSWORD=""
i=1
while [ "$i" -le 10 ]; do
  RESPONSE=$(wget -qO- --header="X-Vault-Token: $VAULT_TOKEN" http://vault:8200/v1/acadconf/data/db_password 2>/dev/null) || true
  MYSQLPASSWORD=$(echo "$RESPONSE" | grep -o '"value":"[^"]*"' | head -1 | sed 's/"value":"\(.*\)"/\1/')
  [ -n "$MYSQLPASSWORD" ] && break
  echo "Vault pas encore prêt, tentative $i/10..." >&2
  sleep 2
  i=$((i + 1))
done

if [ -z "$MYSQLPASSWORD" ]; then
  echo "ERREUR : impossible de récupérer le mot de passe depuis Vault après 10 tentatives" >&2
  echo "Dernière réponse brute : $RESPONSE" >&2
  exit 1
fi

export MYSQLPASSWORD

exec java -Dspring.profiles.active=prod -Djava.security.egd=file:/dev/./urandom -jar app.jar
