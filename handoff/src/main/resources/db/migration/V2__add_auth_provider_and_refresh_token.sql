-- Prepara o app_user para login com provedor externo (ex.: Google),
-- onde o usuário não tem senha, e cria a tabela de refresh tokens.

ALTER TABLE app_user
    ALTER COLUMN password_hash DROP NOT NULL;

ALTER TABLE app_user
    ADD COLUMN auth_provider    VARCHAR(20)  NOT NULL DEFAULT 'LOCAL',
    ADD COLUMN provider_subject VARCHAR(255);

ALTER TABLE app_user
    ADD CONSTRAINT ck_app_user_auth_provider CHECK (auth_provider IN ('LOCAL', 'GOOGLE')),
    -- LOCAL precisa de senha; provedor externo precisa do "sub" do provedor.
    ADD CONSTRAINT ck_app_user_credentials CHECK (
        (auth_provider = 'LOCAL' AND password_hash IS NOT NULL)
        OR (auth_provider <> 'LOCAL' AND provider_subject IS NOT NULL)
    ),
    -- Um mesmo "sub" do Google não pode pertencer a dois usuários.
    ADD CONSTRAINT uk_app_user_provider_subject UNIQUE (auth_provider, provider_subject);

CREATE TABLE refresh_token (
    id           UUID         PRIMARY KEY,
    user_id      UUID         NOT NULL,
    token_hash   VARCHAR(64)  NOT NULL,   -- SHA-256 em hex; o token puro nunca é salvo
    family_id    UUID         NOT NULL,   -- todos os tokens nascidos de um mesmo login
    replaced_by  UUID,                    -- token que substituiu este na rotação
    expires_at   TIMESTAMPTZ  NOT NULL,
    revoked_at   TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_refresh_token_user        FOREIGN KEY (user_id)     REFERENCES app_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_refresh_token_replaced_by FOREIGN KEY (replaced_by) REFERENCES refresh_token (id),
    CONSTRAINT uk_refresh_token_hash        UNIQUE (token_hash)
);

CREATE INDEX ix_refresh_token_user_id   ON refresh_token (user_id);
CREATE INDEX ix_refresh_token_family_id ON refresh_token (family_id);
