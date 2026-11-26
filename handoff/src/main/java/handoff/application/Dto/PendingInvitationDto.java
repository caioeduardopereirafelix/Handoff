package handoff.application.Dto;

import handoff.application.Entity.Invitation;
import handoff.application.Entity.Role;

import java.time.Instant;
import java.util.UUID;

public record PendingInvitationDto(
        UUID id,
        String email,
        Role role,
        String invitedByName,
        Instant expiresAt,
        Instant createdAt
) {

    public static PendingInvitationDto from(Invitation invitation) {
        String invitedBy = invitation.getInvitedBy() == null ? null : invitation.getInvitedBy().getName();
        return new PendingInvitationDto(invitation.getId(), invitation.getEmail(), invitation.getRole(), invitedBy,
                invitation.getExpiresAt(), invitation.getCreatedAt());
    }
}
