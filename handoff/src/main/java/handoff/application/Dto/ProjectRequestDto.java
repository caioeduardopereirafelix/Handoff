package handoff.application.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ProjectRequestDto(
        @NotNull UUID clientId,
        @NotBlank @Size(max = 120) String name
) {
}
