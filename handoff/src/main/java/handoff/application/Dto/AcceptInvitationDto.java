package handoff.application.Dto;

import handoff.application.Validation.MaxBytes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AcceptInvitationDto(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(min = 8)
        @MaxBytes(value = 72, message = "A senha é longa demais. Use até 72 bytes (letras acentuadas contam como 2).") String password
) {
}
