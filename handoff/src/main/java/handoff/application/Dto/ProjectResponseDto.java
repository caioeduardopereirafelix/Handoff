package handoff.application.Dto;

import handoff.application.Entity.Project;
import handoff.application.Entity.ProjectStatus;

import java.time.Instant;
import java.util.UUID;

public record ProjectResponseDto(
        UUID id,
        UUID clientId,
        String clientName,
        String name,
        ProjectStatus status,
        Instant createdAt
) {

    public static ProjectResponseDto from(Project project) {
        return new ProjectResponseDto(project.getId(), project.getClient().getId(), project.getClient().getName(),
                project.getName(), project.getStatus(), project.getCreatedAt());
    }
}
