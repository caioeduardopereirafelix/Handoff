package handoff.application.Dto;

import java.time.Instant;

public record DownloadUrlResponseDto(
        String downloadUrl,
        Instant expiresAt
) {
}
