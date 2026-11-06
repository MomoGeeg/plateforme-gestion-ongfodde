# Backend FODDE — `ongfoddeApp`

API REST de la plateforme FODDE. Voir aussi `../CLAUDE.md` (vue d'ensemble, règles de travail).

## Stack

- Spring Boot 4, Java 21, Spring Security 7 (JWT sans état), JPA/Hibernate, Hikari, Lombok.
- PostgreSQL Supabase via le *session pooler* ; **Flyway désactivé** : le schéma de la base n'est
  pas versionné ici et ne correspond pas toujours aux entités (voir « Pièges »).
- Déploiement : Docker (`Dockerfile`) sur Render, service `fodde-api`, région Frankfurt.
  Déploiement **manuel** (l'utilisateur clique « Manual Deploy »). Plan gratuit : le service
  s'endort après 15 min ; un moniteur UptimeRobot appelle `GET /api/health` pour le garder éveillé.

## Organisation

- `src/main/java/ong/fodde/config/` : sécurité (`SecurityConfig`, `JwtAuthFilter`), CORS, réglages.
- `src/main/java/ong/fodde/modules/<domaine>/{controller,dto,entity,repository,service}` :
  `auth`, `projets`, `financier` (dépenses, avances, budgets annuels, allocations, lignes),
  `missions`, `acquisitions`, `rh` (employés, contrats, congés, évaluations), `se` (indicateurs,
  formulaires), `ged` (documents + fichiers), `alertes`, `rapports`, `logistique`.
- `shared/` : `GlobalExceptionHandler` (erreurs JSON `{ "erreur": "…" }`), `Prechargement`.

## Conventions

- Droits par `@PreAuthorize("hasAuthority('ROLE_…')")` sur chaque route.
- Utilisateur connecté : `@AuthenticationPrincipal User currentUser` (le filtre JWT place
  l'entité `ong.fodde.modules.auth.entity.User`). Préférer cela à un paramètre `userId`
  (plusieurs anciennes routes prennent encore `userId` en paramètre : à ne pas reproduire).
- Erreurs métier : `BusinessException` (→ 400), introuvable : `ResourceNotFoundException` (→ 404),
  accès refusé → 403. **Ne jamais renvoyer 401 pour autre chose qu'un jeton absent ou invalide** :
  le frontend déconnecte l'utilisateur sur un 401.
- Listes : précharger les entités liées avec `Prechargement.precharger(repo, liste, getter)`
  avant de convertir (évite une requête par ligne).

## Pièges connus

- `spring.jpa.properties.hibernate.type.preferred_uuid_jdbc_type=VARCHAR` + URL JDBC avec
  `stringtype=unspecified` : les UUID et les enums partent en texte et PostgreSQL les convertit.
  **Ne pas activer `hibernate.default_batch_fetch_size`** : les tableaux `= any(?)` deviennent
  `varchar[]` et PostgreSQL refuse (`operator does not exist: uuid = character varying`).
- Colonnes de type enum en base (`frequence_collecte`, `niveau_indicateur`…) : envoyer exactement
  les valeurs de l'enum PostgreSQL.
- Le schéma réel se lit dans Supabase (outil MCP Supabase, `information_schema.columns`), pas
  dans les entités.

## Vérifier un changement

- Compiler : `./mvnw -q -o compile` (le mode hors ligne évite une erreur de certificat de Maven).
- Démarrage local sans base possible pour tester la sécurité ou une route sans base de données ;
  les tests contre la vraie base se font en production après déploiement, avec l'utilisateur.
- Variables d'environnement documentées dans `env.example`.
