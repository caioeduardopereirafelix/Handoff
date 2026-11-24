CREATE TABLE activity_log (
    id                UUID         PRIMARY KEY,
    organization_id   UUID         NOT NULL,
    request_id        UUID         NOT NULL,
    actor_type        VARCHAR(10)  NOT NULL,
    actor_user_id     UUID,
    portal_access_id  UUID,
    action            VARCHAR(30)  NOT NULL,
    from_status       VARCHAR(30),
    to_status         VARCHAR(30),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_activity_log_organization FOREIGN KEY (organization_id) REFERENCES organization (id),
    CONSTRAINT fk_activity_log_request FOREIGN KEY (request_id, organization_id) REFERENCES request (id, organization_id) ON DELETE CASCADE,
    CONSTRAINT fk_activity_log_actor_user FOREIGN KEY (actor_user_id) REFERENCES app_user (id) ON DELETE SET NULL,
    CONSTRAINT fk_activity_log_portal_access FOREIGN KEY (portal_access_id) REFERENCES portal_access (id) ON DELETE SET NULL,
    CONSTRAINT ck_activity_log_actor_type CHECK (actor_type IN ('USER', 'CLIENT')),
    CONSTRAINT ck_activity_log_action CHECK (action IN ('REQUEST_CREATED', 'STATUS_CHANGED', 'COMMENT_ADDED', 'ASSIGNMENT_CHANGED'))
);

CREATE INDEX ix_activity_log_request_id ON activity_log (request_id, created_at);
CREATE INDEX ix_activity_log_organization_id ON activity_log (organization_id);

CREATE FUNCTION activity_log_block_update() RETURNS trigger AS $$
BEGIN
    IF NEW.actor_user_id IS DISTINCT FROM OLD.actor_user_id
       AND NEW.actor_user_id IS NULL
       AND (NEW.id, NEW.organization_id, NEW.request_id, NEW.actor_type, NEW.portal_access_id,
            NEW.action, NEW.from_status, NEW.to_status, NEW.created_at)
           IS NOT DISTINCT FROM
           (OLD.id, OLD.organization_id, OLD.request_id, OLD.actor_type, OLD.portal_access_id,
            OLD.action, OLD.from_status, OLD.to_status, OLD.created_at) THEN
        RETURN NEW;
    END IF;
    IF NEW.portal_access_id IS DISTINCT FROM OLD.portal_access_id
       AND NEW.portal_access_id IS NULL
       AND (NEW.id, NEW.organization_id, NEW.request_id, NEW.actor_type, NEW.actor_user_id,
            NEW.action, NEW.from_status, NEW.to_status, NEW.created_at)
           IS NOT DISTINCT FROM
           (OLD.id, OLD.organization_id, OLD.request_id, OLD.actor_type, OLD.actor_user_id,
            OLD.action, OLD.from_status, OLD.to_status, OLD.created_at) THEN
        RETURN NEW;
    END IF;
    RAISE EXCEPTION 'activity_log é somente inserção';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_activity_log_block_update
    BEFORE UPDATE ON activity_log
    FOR EACH ROW EXECUTE FUNCTION activity_log_block_update();
