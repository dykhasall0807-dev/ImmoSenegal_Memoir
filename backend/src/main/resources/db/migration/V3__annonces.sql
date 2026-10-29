-- V3 : annonces immobilières (photos, vérification et visites arrivent dans des migrations ultérieures).

CREATE TABLE annonces (
    id               UUID             PRIMARY KEY,
    proprietaire_id  UUID             NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    titre            VARCHAR(150)     NOT NULL,
    description      VARCHAR(5000),
    type_bien        VARCHAR(20)      NOT NULL CHECK (type_bien IN ('MAISON', 'APPARTEMENT', 'STUDIO', 'VILLA', 'TERRAIN')),
    type_transaction VARCHAR(20)      NOT NULL CHECK (type_transaction IN ('LOCATION', 'VENTE')),
    prix             BIGINT           NOT NULL CHECK (prix > 0),
    superficie       NUMERIC(12, 2)   NOT NULL CHECK (superficie > 0),
    pieces           INTEGER          CHECK (pieces >= 0),
    quartier         VARCHAR(100)     NOT NULL,
    ville            VARCHAR(100)     NOT NULL,
    adresse          VARCHAR(255),
    latitude         NUMERIC(9, 6)    CHECK (latitude BETWEEN -90 AND 90),
    longitude        NUMERIC(10, 6)   CHECK (longitude BETWEEN -180 AND 180),
    statut           VARCHAR(20)      NOT NULL CHECK (statut IN ('DISPONIBLE', 'RESERVE', 'VENDU', 'LOUE')),
    cree_le          TIMESTAMPTZ      NOT NULL DEFAULT now(),
    modifie_le       TIMESTAMPTZ      NOT NULL DEFAULT now(),
    -- Latitude et longitude vont ensemble.
    CONSTRAINT ck_coordonnees_completes CHECK ((latitude IS NULL) = (longitude IS NULL))
);

CREATE INDEX ix_annonces_recherche ON annonces (type_transaction, type_bien, statut, prix);
CREATE INDEX ix_annonces_proprietaire ON annonces (proprietaire_id);
CREATE INDEX ix_annonces_quartier ON annonces (lower(quartier));

CREATE TABLE annonce_equipements (
    annonce_id UUID        NOT NULL REFERENCES annonces (id) ON DELETE CASCADE,
    equipement VARCHAR(50) NOT NULL,
    PRIMARY KEY (annonce_id, equipement)
);
