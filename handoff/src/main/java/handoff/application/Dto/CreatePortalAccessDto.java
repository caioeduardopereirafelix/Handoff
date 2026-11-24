package handoff.application.Dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CreatePortalAccessDto(
        @Min(1) @Max(365) Integer expiresInDays,
        Boolean sendToClient
) {
}
