-- V5 : photos des annonces, stockées en base (bytea) : simple, sauvegardées avec la base, aucun disque à gérer.
-- Limites imposées par l'application : 5 Mo par photo, 8 photos par annonce, JPEG / PNG uniquement (ré-encodées, sans métadonnées).

CREATE TABLE annonce_photos (
    id         UUID         PRIMARY KEY,
    annonce_id UUID         NOT NULL REFERENCES annonces (id) ON DELETE CASCADE,
    contenu    BYTEA        NOT NULL,
    type_mime  VARCHAR(20)  NOT NULL CHECK (type_mime IN ('image/jpeg', 'image/png')),
    ordre      INTEGER      NOT NULL DEFAULT 0,
    cree_le    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX ix_annonce_photos_annonce ON annonce_photos (annonce_id, ordre);
