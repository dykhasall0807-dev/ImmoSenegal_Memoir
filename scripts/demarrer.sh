#!/usr/bin/env bash
# Lance toute l'application (Linux / macOS / Git Bash). Prérequis : Docker.
set -euo pipefail
cd "$(dirname "$0")/.."
FICHIER=.env.complet
NOUVEAU=0

if [ ! -f "$FICHIER" ]; then
  # Secrets aléatoires propres à CET ordinateur : jamais dans Git (le fichier est ignoré).
  ADMIN_MDP="$(openssl rand -hex 12)"
  {
    echo "DB_PASSWORD=$(openssl rand -hex 16)"
    echo "JWT_SECRET=$(openssl rand -hex 48)"
    echo "BOOTSTRAP_ADMIN_EMAIL=admin@immosenegal.local"
    echo "BOOTSTRAP_ADMIN_PASSWORD=$ADMIN_MDP"
  } > "$FICHIER"
  chmod 600 "$FICHIER"
  NOUVEAU=1
fi

docker compose -f docker-compose.complet.yml --env-file "$FICHIER" up --build -d

echo "Attente du démarrage (1 à 2 minutes la première fois)..."
for _ in $(seq 1 90); do
  if curl -fs http://localhost:8080/api/health > /dev/null 2>&1; then
    echo "Application prête : http://localhost:8080"
    echo "Documentation de l'API : http://localhost:8080/swagger-ui.html"
    if [ "$NOUVEAU" = 1 ]; then
      echo "Compte administrateur créé : voir BOOTSTRAP_ADMIN_EMAIL / BOOTSTRAP_ADMIN_PASSWORD dans le fichier $FICHIER"
    fi
    exit 0
  fi
  sleep 2
done
echo "L'application n'a pas démarré à temps. Voir : docker compose -f docker-compose.complet.yml logs api" >&2
exit 1
