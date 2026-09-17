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
  docker compose exec -T vault vault operator init -format=json > "$INIT_FILE"
  chmod 600 "$INIT_FILE"
fi

UNSEAL_KEYS=$(jq -r '.unseal_keys_b64[0:3][]' "$INIT_FILE")
while IFS= read -r key; do
  docker compose exec -T vault vault operator unseal "$key" >/dev/null
done <<< "$UNSEAL_KEYS"

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

echo "Vault pret."
