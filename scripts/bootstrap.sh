#!/bin/bash
set -euo pipefail

# --- Bloc 2 : mise a jour du systeme ---
export DEBIAN_FRONTEND=noninteractive
apt-get update -y
apt-get upgrade -y

# --- Bloc 3 : installation de Docker (depot officiel + cle GPG) ---
apt-get install -y ca-certificates curl gnupg git

install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
chmod a+r /etc/apt/keyrings/docker.asc

echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null

apt-get update -y
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
systemctl enable docker
systemctl start docker

# --- Bloc 4 : pare-feu interne (UFW) ---
apt-get install -y ufw
ufw default deny incoming
ufw default allow outgoing
ufw allow OpenSSH
ufw allow 80/tcp
ufw --force enable

# --- Bloc 5 : durcissement SSH ---
sed -Ei 's/^#?[[:space:]]*PermitRootLogin.*/PermitRootLogin no/' /etc/ssh/sshd_config
sed -Ei 's/^#?[[:space:]]*PasswordAuthentication.*/PasswordAuthentication no/' /etc/ssh/sshd_config

# Neutraliser le reglage cloud-init s'il existe
if [ -f /etc/ssh/sshd_config.d/50-cloud-init.conf ]; then
    sed -Ei 's/^#?[[:space:]]*PasswordAuthentication.*/PasswordAuthentication no/' \
        /etc/ssh/sshd_config.d/50-cloud-init.conf
fi

# Fix : dossier de separation de privileges de sshd, pas toujours recree
mkdir -p /run/sshd
chmod 0755 /run/sshd

sshd -t
systemctl restart ssh
sshd -T | grep -Ei 'passwordauthentication|permitrootlogin'

# --- Bloc 6 : recuperation du projet et premier deploiement ---
git clone https://github.com/hamzaballa05/infra-radmotech.git/opt/infra
cd /opt/infra/docker && docker compose up -d