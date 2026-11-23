package handoff.application.Dto;

import java.util.UUID;

public record MeResponseDto(
        UUID id,
        String name,
        String email,
        String role,
        UUID organizationId,
        String organizationName,
        String organizationSlug
) {
}
