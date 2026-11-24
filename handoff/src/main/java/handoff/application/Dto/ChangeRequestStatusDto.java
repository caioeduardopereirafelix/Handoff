package handoff.application.Dto;

import handoff.application.Entity.RequestStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeRequestStatusDto(
        @NotNull RequestStatus status
) {
}
