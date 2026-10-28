# ImmoSénégal

Plateforme immobilière sénégalaise (location et vente) : projet de mémoire de Licence 3 Ingénierie Informatique, UASZ.
Elle combine les projets 2 (ImmoSénégal) et 14 (E-Courtier) des briefs de projets.

**Stack :** Java 21, Spring Boot 3.4, Spring Security, PostgreSQL 16, Flyway, Swagger/OpenAPI. Frontend : React, TypeScript, Vite, Tailwind, carte Leaflet/OpenStreetMap (sans clé API).

## État du projet

| Élément | État |
|---|---|
| Squelette backend (sécurité, CORS, Swagger, Flyway, `/api/health`) | **Vérifié par la CI** (compilation + 3 tests, le 28/10/2026) |
| Authentification, rôles, annonces, visites, paiements simulés, assistant | À faire |
| Frontend : projet de base, accueil (recherche), catalogue provisoire, page 404 | **Vérifié par la CI** (compilation + 6 tests, le 28/10/2026) |
| Frontend : connexion, inscription, catalogue réel, détail, tableau de bord, carte | À faire |

Un élément n'est marqué « vérifié » que lorsque la CI (onglet « Actions » de GitHub) est verte.

## Prérequis

- Java 21 et Maven 3.9+
- Docker (pour PostgreSQL en local) ou un PostgreSQL 16 déjà installé

## Démarrage en local

```bash
# 1. Variables d'environnement
cp .env.example .env          # puis éditer .env et choisir DB_PASSWORD

# 2. Base de données
docker compose up -d db

# 3. Backend (charge le .env dans le shell puis démarre l'API)
cd backend
set -a && source ../.env && set +a
mvn spring-boot:run
```

Frontend (dans un second terminal) :

```bash
cd frontend
npm install
npm run dev        # http://localhost:5173 (les appels /api sont redirigés vers le backend)
npm test           # tests
npm run build      # compilation de production
```

Vérifications du backend :

- Santé de l'API : <http://localhost:8080/api/health> (doit répondre `"statut": "UP"`)
- Documentation Swagger : <http://localhost:8080/swagger-ui.html>

## Tests

```bash
cd backend
set -a && source ../.env && set +a
mvn test
```

Les tests démarrent l'application complète : la base PostgreSQL doit être lancée. Sur GitHub, la CI le fait automatiquement avec un PostgreSQL jetable.

## Dépannage

- **`DB_PASSWORD` introuvable au démarrage** : le `.env` n'a pas été chargé dans le shell (`set -a && source ../.env && set +a`).
- **Connexion refusée sur le port 5432** : vérifier que la base tourne avec `docker compose ps`.
- **Erreur Flyway « validate failed »** : ne jamais modifier une migration déjà appliquée ; en créer une nouvelle (`V2__...sql`).

## Sécurité

Aucun secret n'est stocké dans le code ni dans Git : tout passe par les variables d'environnement. Le fichier `.env` est ignoré par Git.
