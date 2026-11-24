package handoff.application.Exception;

import lombok.Getter;

import java.time.Duration;

@Getter
public class TooManyRequestsException extends RuntimeException {

    private final Duration retryAfter;

    public TooManyRequestsException(Duration retryAfter) {
        super(message(retryAfter));
        this.retryAfter = retryAfter;
    }

    private static String message(Duration retryAfter) {
        long seconds = Math.max(1, retryAfter.toSeconds());
        if (seconds < 60) {
            return "Muitas tentativas. Aguarde %d segundos e tente de novo.".formatted(seconds);
        }
        long minutes = (seconds + 59) / 60;
        return "Muitas tentativas. Aguarde %d %s e tente de novo.".formatted(minutes, minutes == 1 ? "minuto" : "minutos");
    }
}
