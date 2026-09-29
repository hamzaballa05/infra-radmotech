# Infrastructure DevOps / Cloud - AcadConf

Ce dépôt contient le code d'infrastructure en tant que code (IaC) et la configuration de déploiement pour l'application pilote **AcadConf**, développée au sein de **RADMO TECH**.

L'infrastructure est déployée sur **OVHcloud (Local Zone Rabat)** afin de garantir la souveraineté des données (conformité à la loi 09-08). L'ensemble du provisionnement est automatisé via **Terraform**, et l'application tourne sur une stack **Docker Compose** sécurisée avec **HashiCorp Vault** pour la gestion des secrets.

## 🏗️ Architecture et Technologies

* **Fournisseur Cloud :** OVHcloud (Local Zone Rabat : `AF-NORTH-LZ-RBA-A`)
* **Infrastructure as Code (IaC) :** Terraform (Providers: `ovh`, `openstack`)
* **Conteneurisation :** Docker Engine & Docker Compose
* **Gestion des secrets :** HashiCorp Vault (auto-hébergé, auto-init, auto-unseal)
* **Monitoring & Alerting :** Prometheus, Grafana, cAdvisor, Node Exporter, Alertmanager, Mailhog
* **Reverse Proxy :** Nginx
* **Base de données :** MySQL 8.0
* **Sécurité CI :** GitHub Actions (Trivy Security Scan)

## 📁 Structure du repo

```
.
├── .github/
│   └── workflows/
│       └── trivy-scan.yml              # Scan de sécurité (CI, GitHub Actions)
│
├── terraform/
│   ├── provider.tf                     # Providers ovh + openstack + random
│   ├── variables.tf                    # Variables d'entrée (identifiants, flavor, clé SSH...)
│   ├── security_group.tf               # Security Group (règles SSH/HTTP)
│   ├── instance.tf                     # Instance + mots de passe aléatoires + sauvegarde OVH
│   ├── outputs.tf                      # IP publique, nom instance, mot de passe DB
│   └── volume.tf                       # (réservé, non utilisé)
│
├── scripts/
│   ├── bootstrap.sh.tpl                # Provisioning initial (1er démarrage, templaté)
│   └── deploy.sh                       # Redéploiement (mises à jour)
│
├── docker/
│   ├── docker-compose.yml              # Orchestration des 9 services
│   ├── secrets/                        # Secrets générés au runtime (ignoré par Git)
│   ├── vault/
│   │   ├── config.hcl                  # Configuration du serveur Vault
│   │   ├── db-read-policy.hcl          # Policy lecture seule (mot de passe DB)
│   │   ├── grafana-read-policy.hcl     # Policy lecture seule (mot de passe Grafana)
│   │   ├── fetch-db-secret.sh          # Entrypoint db : récupère le secret depuis Vault
│   │   ├── fetch-app-secret.sh         # Entrypoint app : idem
│   │   ├── fetch-grafana-secret.sh     # Entrypoint grafana : idem
│   │   ├── vault-bootstrap.sh          # Init + descellement de Vault
│   │   ├── install-vault-service.sh    # Installe le service systemd d'auto-unseal
│   │   └── vault-autounseal.service    # Service systemd (descellement à chaque reboot)
│   ├── prometheus/
│   │   ├── prometheus.yml              # Cibles à scraper
│   │   └── alert_rules.yml             # Règles d'alerte
│   ├── alertmanager/
│   │   └── alertmanager.yml            # Routage des alertes vers Mailhog
│   ├── grafana/provisioning/
│   │   ├── datasources/prometheus.yml  # Source de données par défaut
│   │   └── dashboards/                 # Dashboard applicatif provisionné automatiquement
│   └── nginx/
│       └── default.conf                # Reverse proxy vers app:8080
│
├── Acadconf/                           # Application Spring Boot (AcadConf)
│   ├── Dockerfile                      # Build multi-stage, utilisateur non-root
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/acadconf/     # Code source (config, controller, model, repository, service)
│       ├── main/resources/
│       │   ├── application.properties
│       │   ├── application-prod.properties
│       │   ├── data.sql                # Seed des données de démonstration
│       │   ├── static/                 # CSS, JS
│       │   └── templates/              # Vues Thymeleaf
│       └── test/java/com/acadconf/     # Tests unitaires
│
├── .gitignore
└── README.md
```

## 🛡️ Sécurité en profondeur

L'infrastructure applique le principe de défense en profondeur sur 3 couches :

1. **Security Group OVH** : Bloque tout le trafic entrant au niveau réseau, sauf le port `80` (HTTP ouvert) et le port `22` (SSH restreint à l'IP de l'administrateur).
2. **UFW (Uncomplicated Firewall)** : Configuré sur l'hôte au démarrage (mêmes règles).
3. **Docker Networks** : Les services applicatifs (`app`, `db`, `vault`, etc.) n'exposent aucun port sur l'hôte, hormis `nginx`. L'accès aux outils d'administration se fait exclusivement via tunnel SSH.

## ⚙️ Prérequis

Avant de déployer, vous devez disposer de :

* **Terraform** installé sur votre machine locale.
* **Clés API OVH** (`application_key`, `application_secret`, `consumer_key`).
* **Identifiants OpenStack** (utilisateur/mot de passe, à renseigner dans `terraform.tfvars`).
* Une **clé SSH** pour l'accès à l'instance.

## 🚀 Déploiement Rapide

1. **Cloner le dépôt :**
```bash
   git clone https://github.com/hamzaballa05/infra-radmotech.git
   cd infra-radmotech/terraform
```

2. **Configurer les variables :**
   Créez un fichier `terraform.tfvars` (ignoré par Git) pour définir vos variables sensibles (clés SSH, noms d'instance, identifiants OpenStack, etc.).

3. **Authentification OVH :**
```bash
   source ~/.ovh-credentials.sh
```

4. **Provisionner l'infrastructure :**
```bash
   terraform init
   terraform plan
   terraform apply
```
   *Note : les mots de passe pour la base de données et Grafana sont générés aléatoirement par Terraform, puis automatiquement intégrés dans Vault au premier démarrage de l'instance — aucune intervention manuelle.*

5. **Récupérer l'IP publique de l'instance :**
```bash
   terraform output instance_public_ip
```

## 🔌 Accès aux Services

### 1. Application Web
Accessible directement via le port 80 public : `http://<IP_PUBLIQUE>`

### 2. Monitoring (Grafana) et Alerting (Mailhog)
Ces services ne sont **pas exposés sur Internet**. Vous devez ouvrir un tunnel SSH :
```bash
ssh -L 3000:localhost:3000 -L 8025:localhost:8025 ubuntu@<IP_PUBLIQUE>
```
* **Grafana :** `http://localhost:3000` (utilisateur : `admin` | mot de passe : à récupérer via Vault, voir ci-dessous)
* **Mailhog :** `http://localhost:8025`

### 3. Récupérer un mot de passe depuis Vault
Connectez-vous en SSH à l'instance, puis exécutez :
```bash
# Pour Grafana
docker compose -f /opt/infra/docker/docker-compose.yml exec vault vault kv get -field=grafana_password acadconf/grafana_password

# Pour MySQL
docker compose -f /opt/infra/docker/docker-compose.yml exec vault vault kv get -field=db_password acadconf/db_password
```

## 🔄 Cycle de vie (Mise à jour)

Pour redéployer l'application après une mise à jour du code source sans détruire l'infrastructure :
```bash
# Sur l'instance cible :
bash /opt/infra/scripts/deploy.sh
```

