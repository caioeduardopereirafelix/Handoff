-- Clientes e projetos de cada organização. Toda tabela de negócio carrega
-- organization_id, a coluna que isola um tenant do outro.

CREATE TABLE client (
    id               UUID         PRIMARY KEY,
    organization_id  UUID         NOT NULL,
    name             VARCHAR(120) NOT NULL,
    contact_name     VARCHAR(120),
    email            VARCHAR(254),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_client_organization FOREIGN KEY (organization_id) REFERENCES organization (id),
    -- alvo da FK composta de project: garante que o projeto e o cliente
    -- pertencem à mesma organização
    CONSTRAINT uk_client_id_organization UNIQUE (id, organization_id)
);

CREATE INDEX ix_client_organization_id ON client (organization_id);

CREATE TABLE project (
    id               UUID         PRIMARY KEY,
    organization_id  UUID         NOT NULL,
    client_id        UUID         NOT NULL,
    name             VARCHAR(120) NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_project_organization FOREIGN KEY (organization_id) REFERENCES organization (id),
    -- FK composta: o banco recusa um projeto apontando para cliente de outra organização
    CONSTRAINT fk_project_client FOREIGN KEY (client_id, organization_id) REFERENCES client (id, organization_id),
    CONSTRAINT ck_project_status CHECK (status IN ('ACTIVE', 'ARCHIVED'))
);

CREATE INDEX ix_project_organization_id ON project (organization_id);
CREATE INDEX ix_project_client_id ON project (client_id);
