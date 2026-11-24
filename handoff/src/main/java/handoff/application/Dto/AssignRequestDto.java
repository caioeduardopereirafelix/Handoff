package handoff.application.Dto;

import java.time.LocalDate;
import java.util.UUID;

public record AssignRequestDto(
        UUID assigneeId,
        LocalDate dueDate
) {
}
