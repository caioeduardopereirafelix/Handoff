package handoff.application.Dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record UploadUrlResponseDto(
        UUID attachmentId,
        String uploadUrl,
        String method,
        Map<String, String> headers,
        Instant expiresAt
) {
}
