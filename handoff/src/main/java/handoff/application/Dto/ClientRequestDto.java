package handoff.application.Dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClientRequestDto(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 120) String contactName,
        @Email @Size(max = 254) String email
) {
}
