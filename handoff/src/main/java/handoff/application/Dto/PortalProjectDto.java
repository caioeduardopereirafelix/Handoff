package handoff.application.Dto;

import handoff.application.Entity.ProjectStatus;

import java.util.UUID;

public record PortalProjectDto(
        UUID projectId,
        String projectName,
        String clientName,
        ProjectStatus status
) {
}
