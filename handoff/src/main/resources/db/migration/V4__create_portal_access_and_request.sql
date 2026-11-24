ALTER TABLE project
    ADD COLUMN next_request_number INTEGER NOT NULL DEFAULT 1,
    ADD CONSTRAINT uk_project_id_organization UNIQUE (id, organization_id);

CREATE TABLE portal_access (
    id               UUID         PRIMARY KEY,
    organization_id  UUID         NOT NULL,
    project_id       UUID         NOT NULL,
    token_hash       VARCHAR(64)  NOT NULL,
    created_by       UUID,
    expires_at       TIMESTAMPTZ  NOT NULL,
    revoked_at       TIMESTAMPTZ,
    last_used_at     TIMESTAMPTZ,
    last_used_ip     VARCHAR(45),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_portal_access_organization FOREIGN KEY (organization_id) REFERENCES organization (id),
    CONSTRAINT fk_portal_access_project FOREIGN KEY (project_id, organization_id) REFERENCES project (id, organization_id) ON DELETE CASCADE,
    CONSTRAINT fk_portal_access_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
    CONSTRAINT uk_portal_access_token_hash UNIQUE (token_hash)
);

CREATE INDEX ix_portal_access_project_id ON portal_access (project_id);

CREATE TABLE request (
    id               UUID         PRIMARY KEY,
    organization_id  UUID         NOT NULL,
    project_id       UUID         NOT NULL,
    number           INTEGER      NOT NULL,
    title            VARCHAR(160) NOT NULL,
    description      TEXT,
    priority         VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM',
    status           VARCHAR(30)  NOT NULL DEFAULT 'OPEN',
    created_by_type  VARCHAR(10)  NOT NULL,
    assignee_id      UUID,
    due_date         DATE,
    version          BIGINT       NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_request_organization FOREIGN KEY (organization_id) REFERENCES organization (id),
    CONSTRAINT fk_request_project FOREIGN KEY (project_id, organization_id) REFERENCES project (id, organization_id) ON DELETE CASCADE,
    CONSTRAINT fk_request_assignee FOREIGN KEY (assignee_id) REFERENCES app_user (id) ON DELETE SET NULL,
    CONSTRAINT uk_request_project_number UNIQUE (project_id, number),
    CONSTRAINT ck_request_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT ck_request_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'AWAITING_APPROVAL', 'CHANGES_REQUESTED', 'DONE', 'CANCELED')),
    CONSTRAINT ck_request_created_by_type CHECK (created_by_type IN ('USER', 'CLIENT'))
);

CREATE INDEX ix_request_organization_id ON request (organization_id);
CREATE INDEX ix_request_project_id ON request (project_id);
