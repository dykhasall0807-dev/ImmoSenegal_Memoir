# ImmoSénégal

Plateforme immobilière sénégalaise (location et vente) : projet de mémoire de Licence 3 Ingénierie Informatique, UASZ.
Elle combine les projets 2 (ImmoSénégal) et 14 (E-Courtier) des briefs de projets.

**Stack :** Java 21, Spring Boot 3.4, Spring Security, PostgreSQL 16, Flyway, Swagger/OpenAPI. Frontend : React, TypeScript, Vite, Tailwind, carte Leaflet/OpenStreetMap (sans clé API).

## État du projet

| Élément | État |
|---|---|
| Squelette backend (sécurité, CORS, Swagger, Flyway, `/api/health`) | **Vérifié par la CI** (compilation + 3 tests, le 28/10/2026) |
| Authentification JWT (inscription CLIENT, connexion, profil), rôles, premier ADMIN, demandes de rôle validées par ADMIN, journal d'audit | **Vérifié par la CI** (tests backend) |
| Limitation des tentatives de connexion (5 échecs/email, 20/IP, 15 min → 429 + Retry-After) | **Vérifié par la CI** |
| Session longue : cookie HttpOnly de rafraîchissement (jeton à usage unique, rotation, détection de réutilisation), déconnexion côté serveur | **Vérifié par la CI** (62 tests backend, 0 échec ; rechargement de page vérifié en bout en bout) |
| Mot de passe oublié | À faire |
| Annonces (backend) : publication (PROPRIETAIRE/AGENT/ADMIN), modification/suppression par le propriétaire ou un ADMIN, catalogue public, recherche filtrée + pagination + tri | **Vérifié par la CI** (51 tests backend au total, 0 échec, le 29/10/2026) |
| Photos des annonces : envoi par le propriétaire (JPEG/PNG, 5 Mo, 8 max), nettoyage des métadonnées dont le GPS, couverture dans le catalogue, galerie dans le détail | **Vérifié par la CI** (76 tests backend, 53 tests frontend, parcours de bout en bout avec une vraie image) |
| Annonces : carte | À faire |
| Visites, offres, paiements simulés, messagerie, assistant, vérification des annonces | À faire |
| Frontend : accueil, catalogue réel (filtres, tri, pagination), détail d'une annonce, publication / modification / suppression par le propriétaire (« Mes annonces »), demande de rôle par le Client et validation par l'administrateur | **Vérifié par la CI** (compilation + 53 tests) — 40 tests avec API simulée **et** un parcours de bout en bout (navigateur réel + vrai backend + PostgreSQL) en CI : inscription → demande de rôle → validation admin → publication → catalogue → modification → suppression |
| Frontend : pages Connexion, Inscription, Tableau de bord (profil vérifié par `/api/auth/me`), navigation selon la session | Testé avec une API **simulée** (19 tests) ; **pas encore testé contre le vrai backend** |
| Frontend : carte | À faire |

Un élément n'est marqué « vérifié » que lorsque la CI (onglet « Actions » de GitHub) est verte.

**Photos :** stockées dans la base PostgreSQL (aucun disque à gérer, sauvegardées avec les données). Chaque photo est décodée puis ré-encodée à l'envoi : un fichier qui n'est pas une vraie image est refusé, les métadonnées (position GPS, appareil) sont supprimées, l'image est réduite à 1600 px. WebP et SVG sont refusés. Limite connue : pas de réorganisation des photos (la plus ancienne sert de couverture ; pour changer, supprimer et renvoyer).

**Session :** le jeton d'accès (15 min) reste en mémoire du navigateur, jamais dans `localStorage`. Un cookie `HttpOnly` limité à `/api/auth` (illisible par JavaScript) permet de retrouver la session après un rechargement et de renouveler le jeton discrètement. Chaque jeton de rafraîchissement ne sert qu'une fois ; si un ancien jeton est rejoué, toutes les sessions du compte sont révoquées. Seule l'empreinte SHA-256 est stockée en base. La déconnexion révoque le jeton côté serveur.

**Hébergement en ligne (Neon + Render + Vercel) :** la base est sur Neon, le serveur sur Render (`render.yaml`, redéployé à chaque commit dont les vérifications sont vertes), le site sur Vercel (`frontend/vercel.json`). Vercel relaie les appels `/api` vers Render : le navigateur ne parle qu'à une seule adresse, donc le cookie de session reste « même site ». Limites de l'offre gratuite : le serveur Render s'endort après 15 minutes sans visite (premier chargement lent, un message d'attente s'affiche) ; derrière Vercel, la limitation de connexion par adresse IP voit l'IP du relais, donc le compteur par IP est partagé entre tous les visiteurs (le compteur par email reste exact).

**À régler au déploiement :** `REFRESH_COOKIE_SECURE=true` (https) et `CORS_ALLOWED_ORIGINS` = adresse exacte du frontend. Si le frontend et l'API sont sur deux sites différents (ex. Vercel + Render), mettre `REFRESH_COOKIE_SAMESITE=None`.

## Lancer toute l'application (méthode recommandée, une seule commande)

Prérequis : **Docker** uniquement (Docker Desktop sous Windows et macOS). Ni Java, ni Node, ni Maven à installer.

```bash
# Linux, macOS ou Git Bash
bash scripts/demarrer.sh
```

```powershell
# Windows (PowerShell, Docker Desktop démarré)
powershell -ExecutionPolicy Bypass -File scripts\demarrer.ps1
```

Au premier lancement, le script génère au hasard les secrets (mot de passe de la base, clé JWT, premier compte administrateur) dans le fichier `.env.complet`. Ce fichier est ignoré par Git : il n'existe que sur votre ordinateur. Le premier lancement prend 3 à 6 minutes (construction des images).

- Application : http://localhost:8080 (accessible uniquement depuis votre ordinateur)
- Documentation de l'API (Swagger) : http://localhost:8080/swagger-ui.html
- Compte administrateur : voir `BOOTSTRAP_ADMIN_EMAIL` et `BOOTSTRAP_ADMIN_PASSWORD` dans `.env.complet`
- Arrêter : `docker compose -f docker-compose.complet.yml --env-file .env.complet down`
- Tout effacer (données comprises) : même commande avec `down -v`, puis supprimer `.env.complet`

**Vérification :** la CI GitHub exécute `scripts/demarrer.sh` puis le parcours de bout en bout dans un vrai navigateur sur cette application dockerisée. Le script PowerShell est identique dans son principe mais **n'est pas testé automatiquement** (la CI tourne sous Linux).

## Démarrage pour développer (sans Docker complet)

Prérequis de cette méthode :

- Java 21 et Maven 3.9+
- Docker (pour PostgreSQL en local) ou un PostgreSQL 16 déjà installé

## Démarrage en local

```bash
# 1. Variables d'environnement
cp .env.example .env          # puis éditer .env : choisir DB_PASSWORD et générer JWT_SECRET
openssl rand -base64 48       # à copier dans JWT_SECRET

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

## Créer le premier administrateur

Aucun compte ADMIN n'existe par défaut et aucun formulaire public ne permet d'en créer un.
Pour le tout premier démarrage, renseigner dans `.env` (ou dans les variables du serveur) :

```
BOOTSTRAP_ADMIN_EMAIL=votre.email@exemple.com
BOOTSTRAP_ADMIN_PASSWORD=un_mot_de_passe_de_12_caracteres_minimum
```

Démarrer le backend une fois, puis **supprimer ces deux lignes**. Si un ADMIN existe déjà, elles sont ignorées.
Les propriétaires et agents s'inscrivent comme CLIENT, puis demandent leur rôle (`POST /api/auth/role-requests`) ; un ADMIN valide dans `/api/admin/role-requests`.

## Tests

```bash
cd backend
set -a && source ../.env && set +a
mvn test
```

Les tests démarrent l'application complète : la base PostgreSQL doit être lancée. Sur GitHub, la CI le fait automatiquement avec un PostgreSQL jetable.

## Dépannage

- **`JWT_SECRET` manquant ou trop court** : le backend refuse de démarrer (32 caractères minimum).
- **`DB_PASSWORD` introuvable au démarrage** : le `.env` n'a pas été chargé dans le shell (`set -a && source ../.env && set +a`).
- **Connexion refusée sur le port 5432** : vérifier que la base tourne avec `docker compose ps`.
- **Erreur Flyway « validate failed »** : ne jamais modifier une migration déjà appliquée ; en créer une nouvelle (`V2__...sql`).

## Sécurité

**Limites connues de la limitation de connexion :** les compteurs sont gardés en mémoire (un seul serveur ; ils sont remis à zéro au redémarrage). Un attaquant peut bloquer temporairement (15 min) la connexion à un compte en multipliant les échecs sur son email : c'est le compromis accepté contre la devinette de mots de passe. Derrière un proxy (Render), définir `FORWARD_HEADERS_STRATEGY=native`, sinon toutes les requêtes semblent venir de la même IP.

Aucun secret n'est stocké dans le code ni dans Git : tout passe par les variables d'environnement. Le fichier `.env` est ignoré par Git.
