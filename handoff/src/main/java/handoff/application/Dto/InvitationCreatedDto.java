package handoff.application.Dto;

import handoff.application.Entity.Role;

import java.time.Instant;
import java.util.UUID;

public record InvitationCreatedDto(
        UUID id,
        String email,
        Role role,
        String token,
        Instant expiresAt,
        boolean emailSent
) {
}
