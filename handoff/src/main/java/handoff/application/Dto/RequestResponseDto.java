package handoff.application.Dto;

import handoff.application.Entity.ActorType;
import handoff.application.Entity.AppUser;
import handoff.application.Entity.Request;
import handoff.application.Entity.RequestPriority;
import handoff.application.Entity.RequestStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record RequestResponseDto(
        UUID id,
        UUID projectId,
        int number,
        String code,
        String title,
        String description,
        RequestPriority priority,
        RequestStatus status,
        ActorType createdByType,
        UUID assigneeId,
        String assigneeName,
        LocalDate dueDate,
        long version,
        Instant createdAt,
        Instant updatedAt
) {

    public static RequestResponseDto from(Request request) {
        AppUser assignee = request.getAssignee();
        return new RequestResponseDto(
                request.getId(),
                request.getProject().getId(),
                request.getNumber(),
                "#%03d".formatted(request.getNumber()),
                request.getTitle(),
                request.getDescription(),
                request.getPriority(),
                request.getStatus(),
                request.getCreatedByType(),
                assignee == null ? null : assignee.getId(),
                assignee == null ? null : assignee.getName(),
                request.getDueDate(),
                request.getVersion(),
                request.getCreatedAt(),
                request.getUpdatedAt());
    }
}
