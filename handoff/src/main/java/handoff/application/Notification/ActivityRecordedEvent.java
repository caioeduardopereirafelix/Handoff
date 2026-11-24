package handoff.application.Notification;

import handoff.application.Entity.ActivityAction;
import handoff.application.Entity.ActorType;
import handoff.application.Entity.RequestStatus;

import java.util.UUID;

public record ActivityRecordedEvent(
        UUID requestId,
        ActivityAction action,
        ActorType actorType,
        UUID actorUserId,
        RequestStatus fromStatus,
        RequestStatus toStatus
) {
}
