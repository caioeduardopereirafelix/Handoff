package handoff.application.Dto;

public record TokenResponseDto(
        String token,
        String type,
        // validade do token de acesso, em milissegundos
        long expiration,
        String refreshToken
) {
}
