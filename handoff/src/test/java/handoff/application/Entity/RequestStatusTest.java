package handoff.application.Entity;

import handoff.application.Exception.ForbiddenTransitionException;
import handoff.application.Exception.InvalidTransitionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static handoff.application.Entity.RequestStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestStatusTest {

    private static final Map<RequestStatus, Map<RequestStatus, ActorType>> EXPECTED = Map.of(
            OPEN, Map.of(IN_PROGRESS, ActorType.USER, CANCELED, ActorType.USER),
            IN_PROGRESS, Map.of(AWAITING_APPROVAL, ActorType.USER, CANCELED, ActorType.USER),
            AWAITING_APPROVAL, Map.of(DONE, ActorType.CLIENT, CHANGES_REQUESTED, ActorType.CLIENT, CANCELED, ActorType.USER),
            CHANGES_REQUESTED, Map.of(IN_PROGRESS, ActorType.USER, CANCELED, ActorType.USER),
            DONE, Map.of(),
            CANCELED, Map.of()
    );

    static Stream<Arguments> everyPair() {
        return Arrays.stream(values())
                .flatMap(from -> Arrays.stream(values()).map(to -> Arguments.of(from, to)));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("everyPair")
    void transitionTableMatchesTheSpecification(RequestStatus from, RequestStatus to) {
        ActorType expectedActor = EXPECTED.get(from).get(to);

        assertThat(from.canTransitionTo(to)).isEqualTo(expectedActor != null);
        assertThat(from.actorAllowedTo(to).orElse(null)).isEqualTo(expectedActor);
    }

    @Test
    void onlyDoneAndCanceledAreFinal() {
        assertThat(Arrays.stream(values()).filter(RequestStatus::isFinal))
                .containsExactlyInAnyOrderElementsOf(Set.of(DONE, CANCELED));
    }

    @Test
    void requestRejectsInvalidTransitionWith409Exception() {
        Request request = new Request();

        assertThatThrownBy(() -> request.transitionTo(DONE, ActorType.CLIENT))
                .isInstanceOf(InvalidTransitionException.class);
        assertThat(request.getStatus()).isEqualTo(OPEN);
    }

    @Test
    void requestRejectsWrongActorWith403Exception() {
        Request request = new Request();

        assertThatThrownBy(() -> request.transitionTo(IN_PROGRESS, ActorType.CLIENT))
                .isInstanceOf(ForbiddenTransitionException.class);
        assertThat(request.getStatus()).isEqualTo(OPEN);
    }

    @Test
    void requestFollowsTheHappyPath() {
        Request request = new Request();

        request.transitionTo(IN_PROGRESS, ActorType.USER);
        request.transitionTo(AWAITING_APPROVAL, ActorType.USER);
        request.transitionTo(CHANGES_REQUESTED, ActorType.CLIENT);
        request.transitionTo(IN_PROGRESS, ActorType.USER);
        request.transitionTo(AWAITING_APPROVAL, ActorType.USER);
        request.transitionTo(DONE, ActorType.CLIENT);

        assertThat(request.getStatus()).isEqualTo(DONE);
    }
}
