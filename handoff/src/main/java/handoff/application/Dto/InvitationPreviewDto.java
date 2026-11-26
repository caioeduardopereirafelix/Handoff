package handoff.application.Dto;

import java.time.Instant;

public record InvitationPreviewDto(
        String organizationName,
        String invitedByName,
        String email,
        Instant expiresAt
) {
}
