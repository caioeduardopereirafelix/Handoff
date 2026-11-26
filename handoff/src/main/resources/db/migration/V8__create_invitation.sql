CREATE TABLE invitation (
    id               UUID         PRIMARY KEY,
    organization_id  UUID         NOT NULL,
    email            VARCHAR(254) NOT NULL,
    role             VARCHAR(20)  NOT NULL,
    token_hash       VARCHAR(64)  NOT NULL,
    invited_by       UUID,
    expires_at       TIMESTAMPTZ  NOT NULL,
    accepted_at      TIMESTAMPTZ,
    revoked_at       TIMESTAMPTZ,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_invitation_organization FOREIGN KEY (organization_id) REFERENCES organization (id) ON DELETE CASCADE,
    CONSTRAINT fk_invitation_invited_by FOREIGN KEY (invited_by) REFERENCES app_user (id) ON DELETE SET NULL,
    CONSTRAINT uk_invitation_token_hash UNIQUE (token_hash),
    CONSTRAINT ck_invitation_role CHECK (role IN ('OWNER', 'MEMBER'))
);

CREATE INDEX ix_invitation_organization_email ON invitation (organization_id, email);
