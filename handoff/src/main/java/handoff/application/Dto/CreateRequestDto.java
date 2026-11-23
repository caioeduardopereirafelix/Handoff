package handoff.application.Dto;

import handoff.application.Entity.RequestPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRequestDto(
        @NotBlank @Size(max = 160) String title,
        @Size(max = 5000) String description,
        RequestPriority priority
) {
}
