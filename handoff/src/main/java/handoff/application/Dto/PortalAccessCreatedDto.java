package handoff.application.Dto;

import java.time.Instant;
import java.util.UUID;

public record PortalAccessCreatedDto(
        UUID id,
        String token,
        String portalPath,
        Instant expiresAt
) {
}
