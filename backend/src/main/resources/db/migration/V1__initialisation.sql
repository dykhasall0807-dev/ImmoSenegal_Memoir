-- V1 : initialisation du schéma.
-- Cette table minimale permet de vérifier que Flyway et PostgreSQL fonctionnent
-- (utilisée par l'endpoint /api/health). Les tables métier arrivent dans les migrations suivantes.

CREATE TABLE app_meta (
    cle    VARCHAR(100) PRIMARY KEY,
    valeur VARCHAR(255) NOT NULL
);

INSERT INTO app_meta (cle, valeur) VALUES ('schema_version', '1');
