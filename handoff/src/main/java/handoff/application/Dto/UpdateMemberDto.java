package handoff.application.Dto;

import handoff.application.Entity.Role;

public record UpdateMemberDto(
        Role role,
        Boolean active
) {
}
