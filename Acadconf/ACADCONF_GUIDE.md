# Guide de présentation — AcadConf

> Document pédagogique pour comprendre l'application AcadConf de A à Z, écrit pour quelqu'un qui débute en développement logiciel (bases Java, mais jamais travaillé sur une vraie application web connectée à une base de données).

---

## 1. Vue d'ensemble

**AcadConf** est une plateforme web qui gère la soumission d'articles scientifiques pour des conférences académiques (un peu comme un mini "EasyChair" ou "OpenReview"). Un chercheur (**auteur**) peut créer un compte, consulter les conférences ouvertes, et soumettre un article en PDF avant une date limite. Un **président** de conférence crée et gère ses conférences. Un **administrateur** gère l'ensemble des comptes utilisateurs de la plateforme.

Techniquement, c'est une application web **Java / Spring Boot** classique en **3 couches** (voir schéma ci-dessous), qui génère elle-même les pages HTML côté serveur (pas de React/Angular séparé) et qui stocke ses données dans une base **MySQL**.

```
┌─────────────────────────────────────────────────────────────┐
│  NAVIGATEUR (l'utilisateur clique sur un lien/bouton)         │
└───────────────────────────┬─────────────────────────────────┘
                             │ requête HTTP (GET /articles/my, POST /login...)
┌───────────────────────────▼─────────────────────────────────┐
│  COUCHE 1 — CONTRÔLEURS (package controller)                  │
│  Reçoit la requête, appelle le service, choisit la page à     │
│  afficher (ex: ArticleController, ConferenceController)       │
└───────────────────────────┬─────────────────────────────────┘
                             │ appelle des méthodes Java normales
┌───────────────────────────▼─────────────────────────────────┐
│  COUCHE 2 — SERVICES (package service)                        │
│  Contient la LOGIQUE MÉTIER : règles, validations              │
│  (ex: "on ne peut pas soumettre après la deadline")            │
└───────────────────────────┬─────────────────────────────────┘
                             │ appelle des méthodes Java normales
┌───────────────────────────▼─────────────────────────────────┐
│  COUCHE 3 — REPOSITORIES (package repository)                 │
│  Traduit des appels Java en requêtes SQL, parle à la base      │
│  (ex: ArticleRepository.findByAuteurPrincipal(...))            │
└───────────────────────────┬─────────────────────────────────┘
                             │ SQL (SELECT, INSERT, UPDATE...)
┌───────────────────────────▼─────────────────────────────────┐
│  BASE DE DONNÉES MySQL (tables: utilisateur, conference,      │
│  article)                                                      │
└─────────────────────────────────────────────────────────────┘
```

Une 4ᵉ "couche" transversale existe : les **templates HTML** (Thymeleaf, dans `src/main/resources/templates`) — c'est ce que le contrôleur choisit d'afficher à la fin, rempli avec les données du service.

---

## 2. Le modèle de données

### 2.1 Les 3 entités

Une **entité** en Spring, c'est une classe Java annotée `@Entity` qui représente **une table de la base de données**. Chaque instance de la classe = une ligne de la table. C'est le rôle de la technologie **JPA/Hibernate** : elle fait la traduction automatique classe Java ↔ table SQL.

L'application ne modélise que **3 entités** : `Utilisateur`, `Conference`, `Article`.

> **Point important pour ta présentation** : le code contient des traces (voir `DataInitializer.java`, méthode `clearAll()`) de tables `notification`, `decision`, `evaluation`, `affectation` qui existaient dans une version antérieure du projet mais ont été **retirées du modèle actuel**. Aujourd'hui, l'évaluation et la décision sont simplifiées : ce sont juste des **statuts** (`SOUMIS`, `EN_EVALUATION`, `ACCEPTE`, `REJETE`) directement sur l'entité `Article`, pas des tables séparées avec leur propre historique. C'est une simplification volontaire (ou un chantier non terminé) à mentionner à ton encadrant — le workflow d'évaluation par des relecteurs (peer review) avec notes, commentaires et affectation de rapporteurs **n'est pas implémenté**, seul le statut final existe.

#### `Utilisateur` (table `utilisateur`)

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire, auto-générée |
| `nom`, `prenom` | String | Identité |
| `email` | String | Unique, sert d'identifiant de connexion |
| `motDePasse` | String | Mot de passe **haché** (jamais en clair, voir section 3) |
| `affiliation`, `pays` | String | Info académique (ex: "EPFL Lausanne") |
| `role` | Enum `Role` | `ADMIN`, `AUTEUR`, ou `PRESIDENT` |
| `statutCompte` | Enum `StatutCompte` | `ACTIF`, `INACTIF`, `BLOQUE`, `EN_ATTENTE` |
| `dateCreation` | LocalDateTime | Auto-remplie à la création |

#### `Conference` (table `conference`)

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `titre`, `sigle` | String | Ex: "NeurIPS 2025" |
| `description`, `themes` | String (TEXT) | |
| `dateDebut`, `dateFin` | LocalDate | Dates de l'événement |
| `dateLimiteSoumission` | LocalDate | Deadline pour soumettre un article |
| `dateLimiteEvaluation` | LocalDate | Deadline pour évaluer |
| `statut` | Enum `Statut` | `OUVERTE`, `FERMEE`, `EN_EVALUATION`, `TERMINEE` |
| `president` | **relation** → `Utilisateur` | Qui a créé/gère la conférence |
| `articles` | **relation** → liste d'`Article` | Tous les articles soumis à cette conférence |

#### `Article` (table `article`)

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `titre`, `resume`, `motsCles` | String | Métadonnées de l'article |
| `fichierPdf` | String | Nom du fichier PDF stocké sur le disque |
| `dateSoumission` | LocalDateTime | Auto-remplie |
| `statutArticle` | Enum `Statut` | `SOUMIS`, `EN_EVALUATION`, `ACCEPTE`, `REJETE` |
| `conference` | **relation** → `Conference` | À quelle conférence il est soumis |
| `auteurPrincipal` | **relation** → `Utilisateur` | Qui l'a soumis |

### 2.2 Schéma des relations

```
┌──────────────────────┐
│     Utilisateur       │
│  id, nom, prenom,     │
│  email, motDePasse,   │
│  role (ADMIN/AUTEUR/  │
│  PRESIDENT), statut   │
└──────────┬────────────┘
           │
           │ 1 utilisateur (rôle PRESIDENT)
           │ peut présider PLUSIEURS conférences
           │
     ┌─────▼─────────────────────┐
     │        Conference          │        "1 conférence
     │  id, titre, sigle,         │         a PLUSIEURS
     │  dateLimiteSoumission,     │         articles"
     │  statut (OUVERTE/FERMEE…)  │◄────────────────────┐
     │  president → Utilisateur   │                      │
     └─────────────┬───────────────                      │
                    │ 1                                   │
                    │                                      │
                    │ 0..N                                 │
              ┌─────▼──────────────────────┐               │
              │          Article            │──────────────┘
              │  id, titre, resume,         │
              │  fichierPdf,                │
              │  statutArticle (SOUMIS/     │
              │  EN_EVALUATION/ACCEPTE/     │
              │  REJETE)                    │
              │  conference   → Conference  │
              │  auteurPrincipal → Utilisateur │
              └──────────────────────────────┘
                    ▲
                    │ 1 utilisateur (rôle AUTEUR)
                    │ peut soumettre PLUSIEURS articles
                    │
              (même entité Utilisateur que ci-dessus)
```

**En résumé, en langage courant** :
- Un `Utilisateur` avec le rôle `PRESIDENT` **crée** des `Conference` (relation "1 président → plusieurs conférences").
- Une `Conference` **reçoit** des `Article` (relation "1 conférence → plusieurs articles"), tant que la date limite de soumission n'est pas dépassée.
- Un `Utilisateur` avec le rôle `AUTEUR` **soumet** des `Article` (relation "1 auteur → plusieurs articles").
- Un `Article` appartient donc à exactement **une** conférence et a exactement **un** auteur principal — ce sont des relations **`@ManyToOne`** côté `Article` (plusieurs articles peuvent pointer vers la même conférence / le même auteur).

### 2.3 Comment ça se traduit en base de données

Dans une base relationnelle, ces relations objets deviennent des **clés étrangères** (foreign keys) :

```sql
-- table article (simplifiée)
CREATE TABLE article (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    titre               VARCHAR(250) NOT NULL,
    statut_article      VARCHAR(20),
    id_conference       BIGINT NOT NULL REFERENCES conference(id),
    id_auteur_principal BIGINT NOT NULL REFERENCES utilisateur(id)
);
```
`id_conference` et `id_auteur_principal` sont les colonnes qui matérialisent les annotations `@ManyToOne` / `@JoinColumn` que tu verras dans `Article.java` (lignes 37-44).

---

## 3. Les rôles et la sécurité

### 3.1 Les 3 rôles

| Rôle | Ce qu'il peut faire |
|---|---|
| **AUTEUR** | S'inscrire, soumettre un article à une conférence ouverte, voir/modifier/supprimer **ses propres** articles (`/articles/my`, `/articles/submit`) |
| **PRESIDENT** | Créer/modifier des conférences (`/conferences/new`, `/conferences/*/edit`), voir toutes les soumissions (`/articles`) |
| **ADMIN** | Tout ce que fait un président, + gérer tous les comptes utilisateurs (`/admin/**`) : créer, modifier, bloquer, supprimer |

Ces rôles sont définis dans l'enum `Utilisateur.Role` (`model/Utilisateur.java`, ligne 62-64) — **seulement 3 valeurs existent réellement** : `ADMIN`, `AUTEUR`, `PRESIDENT`. Tu remarqueras dans les templates HTML (`fragments/layout.html`) des références à des rôles `RELECTEUR` et `COMITE` (relecteur = reviewer, comité = committee) — ce sont des **résidus d'une version future non terminée** : le code HTML les prévoit mais l'enum Java ne les contient pas, donc ces blocs ne s'afficheront jamais réellement. Bon point à signaler à ton encadrant comme "dette technique visible".

### 3.2 Comment la sécurité est vérifiée concrètement

L'application utilise **Spring Security**, une bibliothèque qui intercepte chaque requête HTTP avant qu'elle n'atteigne un contrôleur, et vérifie si l'utilisateur a le droit d'y accéder. Trois mécanismes coexistent :

**a) Filtrage par URL — `SecurityConfig.java`** (le plus important, le vrai "videur à la porte")

```java
.requestMatchers("/admin/**").hasRole("ADMIN")
.requestMatchers("/articles").hasAnyRole("ADMIN", "PRESIDENT")
.requestMatchers("/articles/submit", "/articles/my").hasAnyRole("AUTEUR")
.requestMatchers("/conferences/new", "/conferences/*/edit").hasAnyRole("PRESIDENT", "ADMIN")
.anyRequest().authenticated()
```
Ça se lit ainsi : "toute URL commençant par `/admin/` exige le rôle ADMIN ; sinon, toute URL nécessite au minimum d'être connecté". C'est vérifié **avant même** que le code du contrôleur ne s'exécute — impossible de contourner en devinant une URL.

**b) Authentification (savoir qui parle) — `AuthService.java`**

`AuthService implements UserDetailsService` : Spring Security appelle `loadUserByUsername(email)` à chaque tentative de connexion. La méthode va chercher l'utilisateur en base (`utilisateurRepository.findByEmail`), vérifie que son compte est `ACTIF` (sinon refuse même avec le bon mot de passe), et renvoie un objet `UserDetails` contenant l'email, le mot de passe **haché**, et le rôle sous la forme `"ROLE_" + role.name()` (ex: `"ROLE_ADMIN"`) — c'est cette convention de préfixe `ROLE_` qui permet à `hasRole("ADMIN")` de fonctionner.

Le mot de passe n'est jamais stocké en clair : `passwordEncoder.encode(...)` (`BCryptPasswordEncoder`, défini dans `SecurityConfig.java`) transforme "1234" en une chaîne illisible du type `$2a$10$N9qo8u...`. À la connexion, Spring Security re-hache le mot de passe saisi et compare les deux hachages — jamais les mots de passe en clair.

**c) Affichage conditionnel dans les pages HTML — attribut `sec:authorize`**

Une fois connecté, les templates Thymeleaf cachent/affichent des boutons selon le rôle, par exemple dans `fragments/layout.html` (ligne 53) :
```html
<li sec:authorize="hasRole('ADMIN')">
  <a th:href="@{/admin/users}">Utilisateurs</a>
</li>
```
⚠️ **Important à comprendre** : ceci n'est qu'un confort visuel (cacher un lien qu'on n'a pas le droit d'utiliser). La vraie sécurité, c'est le point (a) — même si quelqu'un tapait l'URL `/admin/users` directement dans son navigateur sans avoir le lien visible, Spring Security la bloquerait.

**d) Vérification manuelle dans le code métier**

Certaines règles sont trop fines pour être exprimées par URL, donc codées "à la main" dans les contrôleurs/services. Exemple, `ArticleController.delete()` :
```java
if (!article.getAuteurPrincipal().getId().equals(user.getId())) {
    redirectAttributes.addFlashAttribute("error", "Vous n'êtes pas autorisé à supprimer cet article.");
    return "redirect:/articles/my";
}
```
Ça garantit qu'un auteur ne peut supprimer **que ses propres** articles (Spring Security seul ne sait pas faire cette distinction, qui dépend des données, pas juste du rôle).

---

## 4. Cycle de vie d'une fonctionnalité clé : soumission d'un article

C'est le parcours le plus représentatif de l'application. Voici le trajet complet, du clic de l'utilisateur jusqu'à la base de données et retour.

### Étape 1 — L'auteur ouvre le formulaire

- **Navigateur** : clic sur "Soumettre" → requête `GET /articles/submit`
- **Sécurité** : `SecurityConfig` vérifie que l'utilisateur a le rôle `AUTEUR` (sinon redirection vers login/erreur 403)
- **Contrôleur** : `ArticleController.submitForm()` (`controller/ArticleController.java:92-102`)
  ```java
  @GetMapping("/submit")
  public String submitForm(..., Model model) {
      model.addAttribute("article", new Article());
      var conferences = conferenceService.findOuvertes();   // ← appel au service
      model.addAttribute("conferences", conferences);
      return "articles/submit";   // ← nom du template à afficher
  }
  ```
- **Service** : `ConferenceService.findOuvertes()` filtre les conférences dont le statut est `OUVERTE` **et** dont la date limite n'est pas dépassée (`Conference.isSubmissionOpen()`)
- **Repository → SQL** : `ConferenceRepository.findByStatut(Conference.Statut.OUVERTE)` génère en coulisses
  ```sql
  SELECT * FROM conference WHERE statut = 'OUVERTE';
  ```
- **Template affiché** : `templates/articles/submit.html` — un formulaire avec un `<select>` pour choisir la conférence, des champs titre/résumé/mots-clés, et un champ fichier PDF.

### Étape 2 — L'auteur remplit et envoie le formulaire

- **Navigateur** : soumission du `<form>` → requête `POST /articles/submit`, avec le fichier PDF envoyé en `multipart/form-data` (type d'encodage HTTP nécessaire pour uploader des fichiers)
- **Contrôleur** : `ArticleController.submit()` (`ArticleController.java:104-119`)
  ```java
  @PostMapping("/submit")
  public String submit(@ModelAttribute Article article,
                       @RequestParam("fichier") MultipartFile fichier,
                       @RequestParam("conferenceId") Long conferenceId,
                       @AuthenticationPrincipal UserDetails userDetails,
                       RedirectAttributes redirectAttributes) {
      try {
          Utilisateur auteur = authService.findByEmail(userDetails.getUsername());
          articleService.submit(article, fichier, conferenceService.findById(conferenceId), auteur);
          redirectAttributes.addFlashAttribute("success", "Article soumis avec succès.");
      } catch (Exception e) {
          redirectAttributes.addFlashAttribute("error", e.getMessage());
          return "redirect:/articles/submit";
      }
      return "redirect:/articles/my";
  }
  ```
  Spring transforme automatiquement les champs du formulaire HTML en objet Java `Article` (`@ModelAttribute`) — c'est le **data binding**.

- **Service (la vraie logique métier)** : `ArticleService.submit()` (`service/ArticleService.java:56-71`)
  ```java
  public Article submit(Article article, MultipartFile fichier, Conference conference, Utilisateur auteur) {
      if (!conference.isSubmissionOpen()) {
          throw new IllegalStateException("La date limite de soumission est dépassée.");
      }
      validatePdf(fichier);                     // vérifie que c'est bien un PDF < 10 Mo

      String filename = UUID.randomUUID() + ".pdf";   // nom de fichier unique et imprévisible
      fichier.transferTo(uploadPath.resolve(filename)); // écrit le fichier sur le disque serveur

      article.setFichierPdf(filename);
      article.setConference(conference);
      article.setAuteurPrincipal(auteur);
      return articleRepository.save(article);    // ← déclenche l'INSERT SQL
  }
  ```
  Deux choses se passent physiquement : (1) le **fichier PDF** est écrit sur le disque du serveur, dans le dossier `app.upload.dir` (voir `application.properties`), sous un nom généré aléatoirement (pas le nom d'origine — évite les collisions et les problèmes de sécurité de noms de fichiers) ; (2) les **métadonnées** (titre, résumé, chemin du fichier...) sont enregistrées en base via `articleRepository.save(article)`.

- **Repository → SQL** : Hibernate génère automatiquement (car l'entité `Article` a `@PrePersist` qui remplit `dateSoumission` et `statutArticle = SOUMIS`) :
  ```sql
  INSERT INTO article (titre, resume, mots_cles, fichier_pdf, date_soumission,
                        statut_article, id_conference, id_auteur_principal)
  VALUES ('Attention Is All You Need', '...', '...', 'a1b2c3.pdf', NOW(),
          'SOUMIS', 3, 5);
  ```

- **Redirection** : le navigateur est renvoyé (HTTP 302) vers `GET /articles/my` avec un message flash de succès — c'est le pattern **POST-Redirect-GET**, qui évite qu'un rafraîchissement de page (F5) ne resoumette accidentellement le même formulaire.

### Étape 3 — Consultation par le président ou l'admin

- **Navigateur** : `GET /articles` (liste globale, réservée à `PRESIDENT`/`ADMIN` par `SecurityConfig`)
- **Contrôleur** : `ArticleController.allArticles()` appelle `articleService.findAllFiltered(...)`, qui construit une requête paginée avec filtres optionnels (recherche texte, statut, conférence) :
  ```java
  @Query("""
      SELECT a FROM Article a
      WHERE (:search IS NULL OR :search = '' OR LOWER(a.titre) LIKE LOWER(CONCAT('%',:search,'%'))
             OR LOWER(a.auteurPrincipal.nom) LIKE LOWER(CONCAT('%',:search,'%')) ...)
        AND (:statut IS NULL OR a.statutArticle = :statut)
        AND (:conferenceId IS NULL OR a.conference.id = :conferenceId)
      """)
  Page<Article> findAllFiltered(...);
  ```
  Ceci est du **JPQL** (langage de requête orienté objet, très proche du SQL mais qui manipule des entités Java plutôt que des tables) — Hibernate le traduit en SQL réel au moment de l'exécution.
- **Template affiché** : `articles/list.html` — un tableau paginé de tous les articles soumis, avec badges de statut colorés.

### Étape 4 — La décision finale (limite actuelle du projet)

C'est ici que le parcours est **incomplet** dans le code actuel. Il existe bien une méthode `ArticleService.updateStatut(Long articleId, Article.Statut statut)` qui changerait le statut d'un article (`SOUMIS` → `EN_EVALUATION` → `ACCEPTE`/`REJETE`), et elle est même testée unitairement (`ArticleServiceTest.java`), **mais aucun contrôleur ne l'appelle** : il n'existe **aucune route HTTP ni aucun bouton dans l'interface** qui permette à un président de réellement changer le statut d'un article depuis le navigateur. C'est un bon exemple concret à montrer à ton encadrant : la couche service est prête, mais le "fil" contrôleur → template n'a pas encore été branché pour cette action.

Une fois qu'un article a le statut `ACCEPTE` (aujourd'hui uniquement modifiable en base de données directement, ou via les données de démo), il apparaît dans la liste publique `GET /articles/accepted` (`ArticleController.acceptedArticles()`), affichée par `templates/articles/accepted.html`.

---

## 5. Concepts Java/Spring à connaître

Pour chaque mécanisme, l'explication en langage simple + l'endroit exact où il est utilisé dans AcadConf.

### `@Entity` / `@Table` / `@Id` / `@GeneratedValue`
> Dit à Hibernate "cette classe Java représente une table". `@Id` marque la clé primaire, `@GeneratedValue(strategy = GenerationType.IDENTITY)` délègue à MySQL le soin d'incrémenter automatiquement l'`id` (comme `AUTO_INCREMENT`).
Exemple : `Article.java:8-16`.

### `@ManyToOne` / `@OneToMany` / `@JoinColumn`
> Modélisent les relations entre tables (section 2). `@ManyToOne` du côté "plusieurs" (un `Article` a un seul `Conference`), `@OneToMany(mappedBy=...)` du côté "un" pour naviguer dans l'autre sens en Java (`Conference.getArticles()`). `@JoinColumn(name="id_conference")` précise le nom de la colonne de clé étrangère en base.
Exemple : `Conference.java:56` (`@OneToMany(mappedBy = "conference", cascade = CascadeType.ALL)`) — le `cascade = CascadeType.ALL` signifie que si on supprime une conférence, Hibernate supprime aussi automatiquement tous ses articles.

### `@Enumerated(EnumType.STRING)`
> Par défaut, Hibernate stockerait un enum Java comme un simple numéro (0, 1, 2...) en base — illisible et fragile si on réordonne les valeurs. `EnumType.STRING` force le stockage du **nom textuel** (`"ACCEPTE"`, `"SOUMIS"`...), beaucoup plus sûr et lisible directement dans la base.
Exemple : `Article.java:32-34`.

### `@PrePersist`
> Une méthode "callback" exécutée automatiquement par Hibernate **juste avant** l'INSERT en base. Sert à remplir des valeurs par défaut sans que le développeur ait à y penser à chaque appel.
Exemple : `Article.java:46-50` remplit `dateSoumission = LocalDateTime.now()` et le statut par défaut `SOUMIS`.

### `@NotBlank`, `@NotNull`, `@Email`, `@Size` (Bean Validation)
> Règles de validation posées directement sur les champs de l'entité. Combinées à `@Valid` dans un contrôleur, Spring vérifie automatiquement les données envoyées par l'utilisateur **avant** d'exécuter le code métier.
Exemple : `Utilisateur.java:22-24` (`@NotBlank` sur `prenom`) ; utilisé dans `ConferenceController.create()` avec `@Valid @ModelAttribute("conference") Conference conference, BindingResult result` — si `result.hasErrors()`, on réaffiche le formulaire au lieu d'enregistrer.

### `@Repository` (implicite) / `extends JpaRepository<Article, Long>`
> Le concept le plus "magique" pour un débutant : **on écrit une interface Java vide (juste des signatures de méthodes), et Spring Data JPA génère l'implémentation SQL tout seul**, en lisant le nom de la méthode. `findByAuteurPrincipal(Utilisateur auteur)` devient automatiquement `SELECT * FROM article WHERE id_auteur_principal = ?`. Le `<Article, Long>` dit "cette interface travaille sur l'entité `Article`, dont la clé primaire est de type `Long`".
Exemple : `ArticleRepository.java:14`.

### `@Query` (JPQL)
> Quand le nom de méthode ne suffit pas à exprimer une requête complexe (filtres combinés, recherche texte...), on écrit soi-même la requête en JPQL. Voir la requête `findAllFiltered` détaillée en section 4, étape 3.

### `@Service`
> Marque une classe comme faisant partie de la couche "logique métier". Spring la détecte automatiquement au démarrage et crée **une seule instance** partagée dans toute l'application (un "singleton"), qu'il **injecte** ensuite là où elle est demandée.
Exemple : `ArticleService.java:20`.

### Injection de dépendances par constructeur
> Plutôt que de faire `new ArticleService()` partout où on en a besoin (ce qui créerait des dizaines de copies et rendrait le code impossible à tester), on **déclare** dans le constructeur ce dont la classe a besoin, et Spring se charge de fournir automatiquement les bonnes instances au démarrage.
Exemple : `ArticleController.java:29-36` — le contrôleur demande `ArticleService`, `AuthService`, `ConferenceService`, `ArticleRepository` dans son constructeur ; Spring les "injecte" automatiquement.

### `@Controller` / `@RequestMapping` / `@GetMapping` / `@PostMapping`
> `@Controller` dit à Spring "cette classe répond à des requêtes web". `@RequestMapping("/articles")` fixe le préfixe d'URL commun à toute la classe. `@GetMapping`/`@PostMapping` associent une méthode Java précise à une URL + méthode HTTP précise (GET = consulter, POST = envoyer des données/créer/modifier).
Exemple : `ArticleController.java:20-21, 40, 104`.

### `@PathVariable` / `@RequestParam` / `@ModelAttribute`
> Trois façons de récupérer des données dans une requête HTTP : `@PathVariable` lit un morceau de l'URL (`/articles/{id}` → `Long id`), `@RequestParam` lit un paramètre de requête (`?search=texte`), `@ModelAttribute` reconstruit un objet Java entier à partir des champs d'un formulaire.
Exemple : `ArticleController.java:121-122` (`@PathVariable Long id`), `:41-45` (`@RequestParam`), `:105` (`@ModelAttribute Article article`).

### `Model` / retour de `String` (nom de vue)
> `Model` est un sac de données (clé → valeur) transmis du contrôleur vers le template HTML. Le `String` retourné par la méthode (ex: `"articles/list"`) indique à Spring **quel fichier** dans `templates/` afficher, en y injectant les données du `Model`.
Exemple : `ArticleController.java:55-66`.

### `RedirectAttributes` + `redirect:`
> Permet de faire suivre un message ("Article soumis avec succès") **à travers une redirection HTTP**, sans le perdre. Le préfixe `"redirect:/articles/my"` dit à Spring de renvoyer un code HTTP 302 (redirection) plutôt que d'afficher directement une page — c'est le pattern **POST-Redirect-GET** mentionné en section 4.

### `@AuthenticationPrincipal UserDetails userDetails`
> Récupère automatiquement l'utilisateur actuellement connecté (déduit du cookie de session), sans avoir à gérer soi-même la session HTTP à la main.
Exemple : présent dans quasiment toutes les méthodes de `ArticleController`.

### Thymeleaf (`th:text`, `th:if`, `th:each`, `th:href`, `sec:authorize`)
> Un moteur de templates qui transforme un fichier HTML "normal" (ouvrable tel quel dans un navigateur) en page dynamique. `th:text="${article.titre}"` insère la valeur Java, `th:each="a : ${articles}"` boucle sur une liste (équivalent d'un `for` Java), `th:if` affiche conditionnellement un bloc, `sec:authorize` (extension Spring Security) cache/affiche selon le rôle.
Exemple : `templates/articles/detail.html:35` (`th:text="${article.titre}"`).

### `MultipartFile`
> Type Spring qui représente un fichier envoyé par un formulaire HTML (`enctype="multipart/form-data"`). Permet de lire son contenu, son type MIME, sa taille, et de l'écrire sur disque (`.transferTo(...)`).
Exemple : `ArticleService.java:56, 65`.

### `Page<T>` / `Pageable` / `PageRequest`
> Mécanisme de **pagination** intégré à Spring Data : au lieu de renvoyer les 500 articles d'un coup, on demande "la page 2, 10 éléments par page, triés par date". `Page<Article>` contient à la fois les 10 articles et des métadonnées (nombre total de pages, etc.) utilisées par le composant de pagination dans les templates.
Exemple : `ArticleService.findAllFiltered(...)`, `ArticleRepository.java:16`.

### `BCryptPasswordEncoder`
> Algorithme de hachage de mot de passe **volontairement lent** et **salé** (chaque hachage inclut un "sel" aléatoire, donc deux utilisateurs avec le même mot de passe "1234" ont des hachages différents en base). Rend le cassage par force brute très coûteux.
Exemple : `SecurityConfig.java:19-21`.

---

## 6. Perspective cycle de vie logiciel (SDLC)

### Où en est le projet aujourd'hui

| Étape SDLC | État actuel |
|---|---|
| **Conception** | Modèle de données simplifié et fonctionnel (3 entités), mais des traces de conception antérieure plus riche (evaluation, decision, affectation, notification) subsistent dans `DataInitializer` et dans les templates (rôles RELECTEUR/COMITE, lien `/affectations/{id}`) sans être implémentées. À clarifier : est-ce un choix de simplification définitif, ou un chantier à reprendre ? |
| **Développement** | Fonctionnalités cœur opérationnelles : inscription/connexion, création de conférences, soumission/modification/suppression d'articles, gestion des utilisateurs par l'admin, pagination et filtres de recherche. Le workflow de revue par les pairs (affectation de relecteurs, évaluation, décision finale via l'interface) **n'est pas branché** côté contrôleur/vue, bien que la méthode service `updateStatut` existe et soit testée. |
| **Tests** | 3 classes de tests unitaires (`ArticleServiceTest`, `AuthServiceTest`, `ConferenceServiceTest`, ~535 lignes au total), utilisant JUnit 5 + Mockito pour tester la couche service en isolant la base de données (mocks des repositories). Aucun test de contrôleur (pas de `@WebMvcTest`), aucun test d'intégration bout-en-bout, aucun test de la couche sécurité. |
| **Déploiement** | `Dockerfile` multi-étapes (build Maven puis image légère JRE Alpine) + `docker-compose.yml` pour MySQL en local + `railway.toml` pour un déploiement cloud sur Railway avec health check sur `/login`. Profil `prod` (`application-prod.properties`) désactive les logs verbeux et le rechargement à chaud de Thymeleaf. |

### Ce qui manquerait pour une vraie mise en production

1. **Compléter le workflow métier** : brancher `updateStatut` à une vraie route + interface pour que le président puisse effectivement accepter/rejeter un article. Décider si le peer review (affectation de relecteurs, notes, commentaires) doit être réintroduit.
2. **Tests plus larges** : tests de contrôleurs (vérifier les codes HTTP, les redirections, les accès refusés selon le rôle), tests d'intégration avec une vraie base (ex: Testcontainers + MySQL), test de bout en bout du parcours de soumission.
3. **Gestion des secrets** : `application.properties` contient des valeurs par défaut en clair (`root`/`root` pour MySQL) — à éliminer même en fallback, et à gérer via un vrai gestionnaire de secrets en production (actuellement partiellement fait via variables d'environnement `MYSQLUSER`/`MYSQLPASSWORD`, ce qui est le bon réflexe, mais les defaults restent risqués si oubliés).
4. **Nettoyage de la dette visible** : supprimer `layout/base.html` (fichier de layout mort, non utilisé — tout le monde utilise `fragments/layout.html`), retirer les références UI à des rôles/pages qui n'existent pas (`RELECTEUR`, `COMITE`, `/affectations/{id}`), ou les implémenter.
5. **Validation et gestion d'erreurs plus robuste** : certaines méthodes lancent des `RuntimeException` génériques (`"Article introuvable"`) plutôt que des exceptions métier dédiées avec des codes HTTP appropriés (404 plutôt que 500) — pas de `@ExceptionHandler` global visible (`GlobalControllerAdvice` est actuellement une classe vide, prête à être remplie).
6. **CI/CD** : aucun pipeline visible dans le dépôt (pas de `.github/workflows`) — à mettre en place pour lancer les tests + build Docker automatiquement à chaque push.
7. **Observabilité** : pas de métriques ni de endpoint de santé applicatif au-delà du health check basique `/login` utilisé par Railway ; envisager Spring Boot Actuator pour de vraies métriques en production.
8. **Sécurité additionnelle** : pas de limitation du nombre de tentatives de connexion (brute-force), pas de vérification d'email à l'inscription, upload de fichiers vérifié uniquement par `Content-Type` déclaré par le client (`ArticleService.validatePdf`) — un `Content-Type` peut être falsifié ; une vérification plus stricte du contenu réel du fichier serait plus sûre.

### Comment présenter ça à ton encadrant

Le projet est à un stade **"MVP fonctionnel"** (Minimum Viable Product) : le parcours principal auteur → soumission → consultation fonctionne de bout en bout et est testé en partie, mais le cycle complet de revue académique (évaluation par les pairs, décision collégiale, notifications) reste à construire ou à clarifier comme hors-scope. C'est une trajectoire de développement tout à fait normale et saine à ce stade — le message clé est : "l'architecture est propre et extensible (3 couches bien séparées), la fondation permettrait d'ajouter les entités manquantes sans tout réécrire."
