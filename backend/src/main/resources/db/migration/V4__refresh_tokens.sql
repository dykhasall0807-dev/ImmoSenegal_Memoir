-- V4 : jetons de rafraîchissement (session longue). On ne stocke JAMAIS le jeton lui-même, seulement son empreinte SHA-256.

CREATE TABLE refresh_tokens (
    id         UUID        PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    cree_le    TIMESTAMPTZ NOT NULL DEFAULT now(),
    expire_le  TIMESTAMPTZ NOT NULL,
    revoque_le TIMESTAMPTZ
);

CREATE INDEX ix_refresh_tokens_user ON refresh_tokens (user_id);
