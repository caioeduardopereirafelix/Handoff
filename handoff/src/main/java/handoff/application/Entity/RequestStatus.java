package handoff.application.Entity;

import java.util.Map;
import java.util.Optional;

public enum RequestStatus {

    OPEN,
    IN_PROGRESS,
    AWAITING_APPROVAL,
    CHANGES_REQUESTED,
    DONE,
    CANCELED;

    private static final Map<RequestStatus, Map<RequestStatus, ActorType>> TRANSITIONS = Map.of(
            OPEN, Map.of(
                    IN_PROGRESS, ActorType.USER,
                    CANCELED, ActorType.USER),
            IN_PROGRESS, Map.of(
                    AWAITING_APPROVAL, ActorType.USER,
                    CANCELED, ActorType.USER),
            AWAITING_APPROVAL, Map.of(
                    DONE, ActorType.CLIENT,
                    CHANGES_REQUESTED, ActorType.CLIENT,
                    CANCELED, ActorType.USER),
            CHANGES_REQUESTED, Map.of(
                    IN_PROGRESS, ActorType.USER,
                    CANCELED, ActorType.USER),
            DONE, Map.of(),
            CANCELED, Map.of()
    );

    public boolean canTransitionTo(RequestStatus target) {
        return TRANSITIONS.get(this).containsKey(target);
    }

    public Optional<ActorType> actorAllowedTo(RequestStatus target) {
        return Optional.ofNullable(TRANSITIONS.get(this).get(target));
    }

    public boolean isFinal() {
        return TRANSITIONS.get(this).isEmpty();
    }
}
