package handoff.application.Exception;

public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("Refresh token inválido ou expirado");
    }
}
