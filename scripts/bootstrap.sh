#!/bin/bash
set -euo pipefail

============================================
Bloc 1 : gestion des erreurs (déjà en tête)
============================================
============================================
Bloc 2 : mise à jour du système
============================================

export DEBIAN_FRONTEND=noninteractive
apt-get update -y
apt-get upgrade -y

============================================
Bloc 3 : installation de Docker (dépôt officiel + clé GPG)
============================================

apt-get install -y ca-certificates curl gnupg git

install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
chmod a+r /etc/apt/keyrings/docker.asc

echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null

apt-get update -y
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
systemctl enable docker
systemctl start docker

============================================
Bloc 4 : pare-feu interne (UFW)
============================================

apt-get install -y ufw
ufw default deny incoming
ufw default allow outgoing
ufw allow OpenSSH
ufw allow 80/tcp
ufw --force enable

============================================
Bloc 5 : durcissement SSH
============================================

sed -Ei 's/^#?[[:space:]]*PermitRootLogin.*/PermitRootLogin no/' /etc/ssh/sshd_config
sed -Ei 's/^#?[[:space:]]*PasswordAuthentication.*/PasswordAuthentication no/' /etc/ssh/sshd_config

if [ -f /etc/ssh/sshd_config.d/50-cloud-init.conf ]; then
    sed -Ei 's/^#?[[:space:]]*PasswordAuthentication.*/PasswordAuthentication no/' \
        /etc/ssh/sshd_config.d/50-cloud-init.conf
fi

sshd -t
systemctl restart ssh
sshd -T | grep -Ei 'passwordauthentication|permitrootlogin'

============================================
Bloc 6 : récupération du projet et premier déploiement
============================================

git clone <URL_DE_TON_REPO> /opt/infra
cd /opt/infra/docker && docker compose up -d