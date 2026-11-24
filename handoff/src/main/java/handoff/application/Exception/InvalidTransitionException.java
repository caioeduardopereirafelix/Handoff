package handoff.application.Exception;

import handoff.application.Entity.RequestStatus;

public class InvalidTransitionException extends RuntimeException {

    public InvalidTransitionException(RequestStatus from, RequestStatus to) {
        super("Transição inválida: " + from + " → " + to);
    }
}
