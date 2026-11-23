package handoff.application.Dto;

import handoff.application.Entity.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProjectUpdateDto(
        @NotBlank @Size(max = 120) String name,
        @NotNull ProjectStatus status
) {
}
