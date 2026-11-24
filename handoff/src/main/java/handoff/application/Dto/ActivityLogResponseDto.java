package handoff.application.Dto;

import handoff.application.Entity.ActivityAction;
import handoff.application.Entity.ActivityLog;
import handoff.application.Entity.ActorType;
import handoff.application.Entity.RequestStatus;

import java.time.Instant;
import java.util.UUID;

public record ActivityLogResponseDto(
        UUID id,
        ActivityAction action,
        ActorType actorType,
        String actorName,
        RequestStatus fromStatus,
        RequestStatus toStatus,
        Instant createdAt
) {

    public static ActivityLogResponseDto from(ActivityLog log, String clientName) {
        String actorName = log.getActorType() == ActorType.CLIENT
                ? clientName
                : log.getActorUser() == null ? "Usuário removido" : log.getActorUser().getName();
        return new ActivityLogResponseDto(log.getId(), log.getAction(), log.getActorType(), actorName,
                log.getFromStatus(), log.getToStatus(), log.getCreatedAt());
    }
}
