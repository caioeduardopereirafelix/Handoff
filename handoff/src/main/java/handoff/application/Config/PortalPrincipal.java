package handoff.application.Config;

import java.util.UUID;

public record PortalPrincipal(UUID accessId, UUID organizationId, UUID projectId) {
}
