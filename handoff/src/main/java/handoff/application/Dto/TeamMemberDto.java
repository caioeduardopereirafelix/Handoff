package handoff.application.Dto;

import handoff.application.Entity.AppUser;
import handoff.application.Entity.Role;

import java.time.Instant;
import java.util.UUID;

public record TeamMemberDto(
        UUID id,
        String name,
        String email,
        Role role,
        boolean active,
        Instant createdAt
) {

    public static TeamMemberDto from(AppUser user) {
        return new TeamMemberDto(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.isActive(),
                user.getCreatedAt());
    }
}
