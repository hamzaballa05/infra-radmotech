# AcadConf — Carte DevOps / Infra

Document opérationnel pour déployer, conteneuriser et maintenir AcadConf (Spring Boot 3.3.5 / Java 21 / MySQL). Ne couvre pas la logique métier.

---

## 1. Arbre du projet commenté

```
Acadconf/
├── pom.xml                          → dépendances Maven + build (Java 21, Spring Boot 3.3.5, packagé en .jar)
├── Dockerfile                       → build multi-étapes : compile le jar avec Maven, puis l'exécute dans une image JRE Alpine légère
├── docker-compose.yml                → lance UNIQUEMENT une base MySQL locale pour le dev — ne définit PAS de service pour l'appli elle-même
├── railway.toml                     → config de déploiement Railway (build via Dockerfile, health check HTTP)
├── ACADCONF_GUIDE.md                → doc pédagogique sur le fonctionnement interne du code (pas utile pour l'infra)
├── nohup.out                        → log résiduel d'une exécution manuelle locale (`nohup ... &`), pas généré par le conteneur — à supprimer
├── .gitignore                       → exclut target/, uploads/, *.log, *.class du dépôt
│
├── src/main/java/com/acadconf/
│   ├── AcadConfApplication.java     → point d'entrée (main()), démarre le serveur Tomcat embarqué
│   ├── config/
│   │   ├── SecurityConfig.java      → définit quelles URLs sont publiques / protégées et par quel rôle
│   │   ├── WebConfig.java           → expose le dossier d'upload disque comme route HTTP `/uploads/**`
│   │   └── DataInitializer.java     → jeu de données de démo — ne s'exécute QUE si le profil actif est "dev"
│   ├── controller/                  → reçoit les requêtes HTTP, aucune logique métier ni accès disque/DB direct
│   ├── service/                     → logique métier ; c'est ICI que se fait l'écriture des PDF sur disque (ArticleService)
│   ├── repository/                  → accès MySQL via Spring Data JPA (interfaces générées, pas de SQL à la main)
│   └── model/                       → entités JPA = tables MySQL (schéma auto-géré par Hibernate, ddl-auto=update)
│
├── src/main/resources/
│   ├── application.properties       → config de base, toujours chargée en premier, lit les variables d'environnement
│   ├── application-prod.properties  → surcharges actives seulement si le profil "prod" est actif (voir section 5)
│   ├── static/css/, static/js/      → assets front statiques, servis tels quels par Spring
│   ├── static/uploads/*.pdf         → ⚠️ fichiers PDF de test présents dans le code source lui-même (pas le dossier d'upload réel) — voir section 3
│   └── templates/                   → vues Thymeleaf (HTML serveur), non pertinent pour l'infra
│
└── src/test/java/                   → tests unitaires JUnit, exécutés au build Maven (`mvn package`), absents du jar final
```

---

## 2. Points de configuration externe

| Config | Déclaré dans | Valeur par défaut | Var d'env qui l'override | Si la ressource est indisponible |
|---|---|---|---|---|
| URL base MySQL | `application.properties` L2 | `jdbc:mysql://localhost:3306/acadconf` | `MYSQLHOST`, `MYSQLPORT`, `MYSQLDATABASE` | MySQL injoignable → **l'app ne démarre pas** (échec au boot, `Communications link failure`). Railway relance jusqu'à 3 fois (`railway.toml`) puis abandonne. |
| Identifiants MySQL | `application.properties` L3-4 | `root` / `root` | `MYSQLUSER`, `MYSQLPASSWORD` | Mauvais identifiants → même effet : échec au démarrage, pas de retry utile sans correction manuelle. |
| Port HTTP de l'app | `application.properties` L32 | `8080` | `PORT` | Si le port est déjà occupé sur l'hôte/conteneur → l'app ne démarre pas (bind exception). Railway/Docker gèrent normalement ça automatiquement. |
| Dossier d'upload PDF | `application.properties` L27 (défaut `./uploads/`), surchargé par `application-prod.properties` (`/tmp/uploads/`) | `./uploads/` (dev) / `/tmp/uploads/` (prod) | `UPLOAD_DIR` | Dossier absent → **auto-créé** par le code (`Files.createDirectories`) à chaque soumission, donc pas de crash. Si le filesystem est en lecture seule ou sans permission → erreur 500 uniquement sur l'action de soumission/édition, l'app continue de tourner. |
| Profil Spring actif | Positionné par `Dockerfile` (`-Dspring.profiles.active=prod`) | aucun profil (= dev-like) si lancé hors Docker | `SPRING_PROFILES_ACTIVE` | Détermine quel fichier de properties surcharge l'autre (voir section 5) et si `DataInitializer` s'exécute. |
| Stockage des sessions | `application.properties` L29-31 (`spring.session.store-type=jdbc`) | table auto-créée en base | — | Dépend de MySQL : si la DB tombe pendant que l'app tourne, les utilisateurs sont déconnectés / erreurs 500 sur les requêtes authentifiées. |
| Taille max upload | `application.properties` L23-25 | 10 Mo | — (codé en dur) | Pas une dépendance externe, mais génère une erreur applicative propre (pas de crash) si dépassée. |
| Logs applicatifs | aucune config `logging.file.*` | sortie **stdout/stderr uniquement** | — | Aucun fichier de log à monter en volume ; c'est Docker/Railway qui capte stdout. `nohup.out` à la racine est un résidu d'exécution manuelle, pas un comportement du conteneur. |

**Point d'attention supplémentaire** : `DataInitializer` (compte admin + données de démo) ne tourne qu'avec `@Profile("dev")`. En prod (profil forcé par le Dockerfile), **aucun compte admin n'est créé automatiquement** — l'inscription publique (`/register`) ne crée que des comptes `AUTEUR`. Il faut prévoir un moyen manuel de créer le premier compte ADMIN (insertion SQL directe, ou changer temporairement le profil actif).

---

## 3. Ce qui écrit sur le disque

| Quoi | Chemin réel utilisé en pratique | Persistant ? |
|---|---|---|
| PDF soumis par les auteurs | `${app.upload.dir}` → **`/tmp/uploads/`** en prod (valeur imposée par `application-prod.properties`, active dès que le Dockerfile démarre le jar), `./uploads/` en dev | ⚠️ **Non** : `/tmp` est éphémère dans un conteneur — un redémarrage/redéploiement efface tous les PDF déjà soumis. **Aucun volume Docker n'est défini nulle part dans le repo** (ni `docker-compose.yml`, ni `railway.toml`) → à ajouter si la persistance des PDF est requise. |
| Fichiers de test `static/uploads/*.pdf` | `src/main/resources/static/uploads/` | Compilés **dans le jar** (ressource classpath), copiés dans l'image Docker à chaque build. ⚠️ Ils sont en pratique **inaccessibles en HTTP** : la route `/uploads/**` est interceptée par le handler explicite de `WebConfig` qui pointe vers le dossier disque externe, pas vers le classpath. Ils ne servent donc à rien en prod, juste à alourdir l'image — à supprimer du repo. |
| Logs | Aucun fichier — stdout/stderr uniquement (config par défaut Spring Boot) | Géré par le driver de logs du conteneur/plateforme, pas de volume à prévoir pour les logs. |
| Sessions HTTP | Stockées en base MySQL (`spring.session.store-type=jdbc`), pas sur disque local | Persistantes tant que MySQL l'est. |
| Exports Excel/PDF | Dépendances Apache POI et OpenPDF présentes dans `pom.xml` mais **non utilisées actuellement** dans le code (aucun controller/service ne les appelle) | N/A pour l'instant. |

**Volume Docker recommandé** : monter un volume sur `/tmp/uploads` (ou fixer `UPLOAD_DIR` vers un chemin monté, ex. `/data/uploads`) si les PDF doivent survivre à un redéploiement.

---

## 4. Ports et endpoints exposés

- **Port HTTP principal** : `8080` en interne (`EXPOSE 8080` dans le Dockerfile), configurable via la variable d'env `PORT` (Railway l'injecte automatiquement et route son edge réseau dessus).
- **MySQL (dev local uniquement)** : conteneur sur `3306`, exposé sur l'hôte en `3307` via `docker-compose.yml` (évite un conflit avec un MySQL déjà installé localement sur 3306).
- **Route de fichiers statiques sensible : `/uploads/**`**
  - Sert directement les PDF depuis le disque (`app.upload.dir`) — c'est cette route qu'un navigateur utilise pour afficher/télécharger un article soumis.
  - **Aucun contrôle d'accès** : cette route est dans la liste `permitAll()` de `SecurityConfig` — n'importe qui, même non connecté, peut télécharger un PDF s'il connaît (ou devine) son nom de fichier UUID. La seule protection est l'imprévisibilité du nom de fichier généré, pas une vérification de droits.
- **Autres routes statiques publiques** : `/css/**`, `/js/**`, `/images/**` — sans intérêt sécurité, assets normaux.
- **Health check** : `GET /login` (défini dans `railway.toml`), public, ne vérifie que le rendu de la page de connexion — pas de endpoint Actuator/health applicatif dédié.

---

## 5. Dépendances entre fichiers de config

Ordre de lecture au démarrage d'un conteneur en prod (via Railway) :

```
① Dockerfile
   └─ fixe -Dspring.profiles.active=prod avant même que Spring ne démarre
      (décide QUEL fichier de properties va surcharger l'autre)

② railway.toml
   └─ dit à Railway de builder via ce Dockerfile + healthcheck sur /login
      ne fixe aucune property Spring lui-même, mais la plateforme Railway
      injecte des variables d'env (PORT, MYSQLHOST, MYSQLUSER, ... si un
      plugin MySQL est attaché) que les fichiers .properties lisent ensuite

③ application.properties   (TOUJOURS chargé en premier, base commune)
        │
        ▼ surchargé par, uniquement si profil = prod
④ application-prod.properties   (redéfinit : cache Thymeleaf, niveaux de
                                  log, app.upload.dir → /tmp/uploads/)
        │
        ▼ surchargé par, quel que soit le profil
⑤ Variables d'environnement réelles (${MYSQLHOST:...}, ${PORT:...}, etc.)
   → priorité maximale, appliquées par-dessus les deux fichiers ci-dessus
```

**Priorité effective (la plus forte en premier) :**
Variables d'environnement système > `application-prod.properties` (seulement si `profil=prod`) > `application.properties` (valeurs par défaut).

**En dev local** (`docker-compose up` pour MySQL + `mvn spring-boot:run` sans profil) : seul `application.properties` s'applique, aucun profil n'est actif, donc `DataInitializer` s'exécute (compte admin + données de démo créés automatiquement) et `app.upload.dir` vaut `./uploads/` relatif au répertoire de lancement du process Java.
