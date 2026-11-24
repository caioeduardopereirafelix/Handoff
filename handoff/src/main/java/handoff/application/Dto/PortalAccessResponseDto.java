package handoff.application.Dto;

import handoff.application.Entity.PortalAccess;

import java.time.Instant;
import java.util.UUID;

public record PortalAccessResponseDto(
        UUID id,
        Instant createdAt,
        Instant expiresAt,
        Instant revokedAt,
        Instant lastUsedAt,
        boolean active
) {

    public static PortalAccessResponseDto from(PortalAccess access, Instant now) {
        return new PortalAccessResponseDto(access.getId(), access.getCreatedAt(), access.getExpiresAt(),
                access.getRevokedAt(), access.getLastUsedAt(), access.isActive(now));
    }
}
