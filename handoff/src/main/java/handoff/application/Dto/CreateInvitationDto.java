package handoff.application.Dto;

import handoff.application.Entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateInvitationDto(
        @NotBlank @Email @Size(max = 254) String email,
        Role role
) {
}
