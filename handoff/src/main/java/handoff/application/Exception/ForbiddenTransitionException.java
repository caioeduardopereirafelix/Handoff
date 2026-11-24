package handoff.application.Exception;

import handoff.application.Entity.RequestStatus;

public class ForbiddenTransitionException extends RuntimeException {

    public ForbiddenTransitionException(RequestStatus from, RequestStatus to) {
        super("Você não pode mudar a solicitação de " + from + " para " + to);
    }
}
