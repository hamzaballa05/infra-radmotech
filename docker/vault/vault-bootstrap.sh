#!/bin/bash
# Tourne sur l'HOTE (l'instance OVH), pas dans un conteneur.
# Rend Vault operationnel sans intervention humaine.
#
# LIMITE ASSUMEE : les cles de descellement et le root token sont stockes
# sur le MEME disque que les donnees qu'ils protegent. Ce n'est PAS ce que
# ferait une vraie production (KMS cloud externe requis) -- compromis
# pragmatique documente pour ce projet de stage.

set -euo pipefail
cd /opt/infra/docker

INIT_FILE="/opt/infra/docker/vault/init-keys.json"
TOKEN_FILE="/opt/infra/docker/secrets/vault_app_token.txt"

mkdir -p /opt/infra/docker/secrets

echo "Attente que l'API Vault reponde..."
for i in $(seq 1 30); do
  docker compose exec -T vault vault status >/dev/null 2>&1 && break
  sleep 2
done

if [ -f "$INIT_FILE" ]; then
  echo "Vault deja initialise sur cette instance -- descellement uniquement."
else
  echo "Premiere initialisation de Vault sur cette instance."
  INIT_OUTPUT=$(docker compose exec -T vault vault operator init -format=json)
  echo "$INIT_OUTPUT" > "$INIT_FILE"
  chmod 600 "$INIT_FILE"
fi

mapfile -t UNSEAL_KEYS_ARRAY < <(jq -r '.unseal_keys_b64[0:3][]' "$INIT_FILE")

for attempt in $(seq 1 5); do
  for key in "${UNSEAL_KEYS_ARRAY[@]}"; do
    docker compose exec -T vault vault operator unseal "$key" >/dev/null
  done

SEALED_STATUS=$(docker compose exec -T vault vault status -format=json 2>/dev/null | jq -r '.sealed' || echo "")
  if [ "$SEALED_STATUS" = "false" ]; then
    echo "Vault descelle avec succes (tentative $attempt)."
    break
  fi

  echo "Vault toujours scelle apres tentative $attempt, nouvel essai dans 5s..."
  sleep 5
done

if [ "$SEALED_STATUS" != "false" ]; then
  echo "ERREUR : impossible de desceller Vault apres 5 tentatives" >&2
  exit 1
fi

ROOT_TOKEN=$(jq -r '.root_token' "$INIT_FILE")

if [ ! -f "$TOKEN_FILE" ]; then
  echo "Configuration initiale du secret et de la policy..."
  DB_PASSWORD_VALUE=$(cat /opt/infra/docker/secrets/db_password.txt)
  docker compose exec -T -e VAULT_TOKEN="$ROOT_TOKEN" vault vault secrets enable -path=acadconf kv-v2
  docker compose exec -T -e VAULT_TOKEN="$ROOT_TOKEN" vault vault kv put acadconf/db_password value="$DB_PASSWORD_VALUE"
  docker compose exec -T -e VAULT_TOKEN="$ROOT_TOKEN" vault vault policy write db-reader /vault/config/db-read-policy.hcl
  docker compose exec -T -e VAULT_TOKEN="$ROOT_TOKEN" vault vault token create -type=service -policy="db-reader" -ttl=768h -display-name="acadconf-services" -field=token > "$TOKEN_FILE"
  chmod 600 "$TOKEN_FILE"
  echo "Vault entierement configure."
else
  echo "Token applicatif deja present -- rien a reconfigurer."
fi

GRAFANA_TOKEN_FILE="/opt/infra/docker/secrets/vault_grafana_token.txt"

if [ ! -f "$GRAFANA_TOKEN_FILE" ]; then
  echo "Configuration initiale du secret Grafana et de sa policy..."
  GRAFANA_PASSWORD_VALUE=$(cat /opt/infra/docker/secrets/grafana_password.txt)
  docker compose exec -T -e VAULT_TOKEN="$ROOT_TOKEN" vault vault kv put acadconf/grafana_password value="$GRAFANA_PASSWORD_VALUE"
  docker compose exec -T -e VAULT_TOKEN="$ROOT_TOKEN" vault vault policy write grafana-reader /vault/config/grafana-read-policy.hcl
  docker compose exec -T -e VAULT_TOKEN="$ROOT_TOKEN" vault vault token create -type=service -policy="grafana-reader" -ttl=768h -display-name="grafana-service" -field=token > "$GRAFANA_TOKEN_FILE"
  chmod 600 "$GRAFANA_TOKEN_FILE"
  echo "Vault (Grafana) entierement configure."
else
  echo "Token Grafana deja present -- rien a reconfigurer."
fi

echo "Vault pret."
