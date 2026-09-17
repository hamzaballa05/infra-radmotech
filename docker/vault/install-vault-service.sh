#!/bin/bash
# Installe le service systemd d'auto-unseal Vault.
# A executer UNE SEULE FOIS, apres le premier deploiement.
# Responsabilite unique : mettre en place l'automatisation Vault,
# separee de la preparation d'environnement (bootstrap.sh).
set -euo pipefail

cp /opt/infra/docker/vault/vault-autounseal.service /etc/systemd/system/
systemctl daemon-reload
systemctl enable vault-autounseal.service
systemctl start vault-autounseal.service

echo "Service vault-autounseal installe et démarré."
