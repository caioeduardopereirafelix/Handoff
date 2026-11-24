ALTER TABLE request
    ADD CONSTRAINT uk_request_id_organization UNIQUE (id, organization_id);

CREATE TABLE comment (
    id                UUID         PRIMARY KEY,
    organization_id   UUID         NOT NULL,
    request_id        UUID         NOT NULL,
    author_type       VARCHAR(10)  NOT NULL,
    author_user_id    UUID,
    portal_access_id  UUID,
    body              TEXT         NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_comment_organization FOREIGN KEY (organization_id) REFERENCES organization (id),
    CONSTRAINT fk_comment_request FOREIGN KEY (request_id, organization_id) REFERENCES request (id, organization_id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_author_user FOREIGN KEY (author_user_id) REFERENCES app_user (id) ON DELETE SET NULL,
    CONSTRAINT fk_comment_portal_access FOREIGN KEY (portal_access_id) REFERENCES portal_access (id) ON DELETE SET NULL,
    CONSTRAINT ck_comment_author_type CHECK (author_type IN ('USER', 'CLIENT'))
);

CREATE INDEX ix_comment_request_id ON comment (request_id, created_at);
CREATE INDEX ix_comment_organization_id ON comment (organization_id);
