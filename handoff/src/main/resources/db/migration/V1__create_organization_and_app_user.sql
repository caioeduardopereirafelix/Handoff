CREATE TABLE organization (
    id          UUID         PRIMARY KEY,
    name        VARCHAR(120) NOT NULL,
    slug        VARCHAR(80)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_organization_slug UNIQUE (slug)
);

CREATE TABLE app_user (
    id               UUID         PRIMARY KEY,
    organization_id  UUID         NOT NULL,
    name             VARCHAR(120) NOT NULL,
    email            VARCHAR(254) NOT NULL,
    password_hash    VARCHAR(100) NOT NULL,
    role             VARCHAR(20)  NOT NULL,
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_app_user_organization FOREIGN KEY (organization_id) REFERENCES organization (id),
    CONSTRAINT uk_app_user_email UNIQUE (email),
    CONSTRAINT ck_app_user_role CHECK (role IN ('OWNER', 'MEMBER'))
);

