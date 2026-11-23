package handoff.application.Dto;

public record TokenResponseDto(
        String token,
        String type,
        long expiration,
        String refreshToken
) {
}
