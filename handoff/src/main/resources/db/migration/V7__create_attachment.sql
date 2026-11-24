CREATE TABLE attachment (
    id                   UUID          PRIMARY KEY,
    organization_id      UUID          NOT NULL,
    request_id           UUID          NOT NULL,
    comment_id           UUID,
    storage_key          VARCHAR(300)  NOT NULL,
    original_name        VARCHAR(255)  NOT NULL,
    content_type         VARCHAR(120)  NOT NULL,
    size_bytes           BIGINT        NOT NULL,
    status               VARCHAR(10)   NOT NULL DEFAULT 'PENDING',
    uploaded_by_type     VARCHAR(10)   NOT NULL,
    uploaded_by_user_id  UUID,
    portal_access_id     UUID,
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    completed_at         TIMESTAMPTZ,
    CONSTRAINT fk_attachment_organization FOREIGN KEY (organization_id) REFERENCES organization (id),
    CONSTRAINT fk_attachment_request FOREIGN KEY (request_id, organization_id) REFERENCES request (id, organization_id) ON DELETE CASCADE,
    CONSTRAINT fk_attachment_comment FOREIGN KEY (comment_id) REFERENCES comment (id) ON DELETE SET NULL,
    CONSTRAINT fk_attachment_uploaded_by_user FOREIGN KEY (uploaded_by_user_id) REFERENCES app_user (id) ON DELETE SET NULL,
    CONSTRAINT fk_attachment_portal_access FOREIGN KEY (portal_access_id) REFERENCES portal_access (id) ON DELETE SET NULL,
    CONSTRAINT uk_attachment_storage_key UNIQUE (storage_key),
    CONSTRAINT ck_attachment_status CHECK (status IN ('PENDING', 'READY')),
    CONSTRAINT ck_attachment_uploaded_by_type CHECK (uploaded_by_type IN ('USER', 'CLIENT')),
    CONSTRAINT ck_attachment_size CHECK (size_bytes > 0)
);

CREATE INDEX ix_attachment_request_id ON attachment (request_id);
CREATE INDEX ix_attachment_organization_id ON attachment (organization_id);

ALTER TABLE activity_log DROP CONSTRAINT ck_activity_log_action;
ALTER TABLE activity_log ADD CONSTRAINT ck_activity_log_action
    CHECK (action IN ('REQUEST_CREATED', 'STATUS_CHANGED', 'COMMENT_ADDED', 'ASSIGNMENT_CHANGED', 'ATTACHMENT_ADDED'));
