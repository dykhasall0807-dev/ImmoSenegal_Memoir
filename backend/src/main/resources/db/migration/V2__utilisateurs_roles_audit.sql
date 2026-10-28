-- V2 : comptes utilisateurs, demandes de rôle et journal d'audit.

CREATE TABLE users (
    id            UUID         PRIMARY KEY,
    email         VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    nom_complet   VARCHAR(120) NOT NULL,
    telephone     VARCHAR(20),
    role          VARCHAR(20)  NOT NULL CHECK (role IN ('CLIENT', 'PROPRIETAIRE', 'AGENT', 'ADMIN')),
    actif         BOOLEAN      NOT NULL DEFAULT TRUE,
    cree_le       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE demandes_role (
    id           UUID        PRIMARY KEY,
    user_id      UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_demande VARCHAR(20) NOT NULL CHECK (role_demande IN ('PROPRIETAIRE', 'AGENT')),
    statut       VARCHAR(20) NOT NULL CHECK (statut IN ('EN_ATTENTE', 'APPROUVEE', 'REFUSEE')),
    cree_le      TIMESTAMPTZ NOT NULL DEFAULT now(),
    decide_le    TIMESTAMPTZ,
    decide_par   UUID REFERENCES users (id)
);

-- Une seule demande en attente par utilisateur.
CREATE UNIQUE INDEX ux_demande_en_attente ON demandes_role (user_id) WHERE statut = 'EN_ATTENTE';

-- Journal d'audit : jamais de mot de passe ni de jeton dans cette table.
CREATE TABLE audit_events (
    id      BIGSERIAL    PRIMARY KEY,
    type    VARCHAR(50)  NOT NULL,
    user_id UUID,
    details VARCHAR(500),
    cree_le TIMESTAMPTZ  NOT NULL DEFAULT now()
);
