package handoff.application.Service;

import handoff.application.Config.PortalPrincipal;
import handoff.application.Config.TenantContext;
import handoff.application.Dto.ActivityLogResponseDto;
import handoff.application.Entity.ActivityAction;
import handoff.application.Entity.ActivityLog;
import handoff.application.Entity.ActorType;
import handoff.application.Entity.AppUser;
import handoff.application.Entity.Request;
import handoff.application.Entity.RequestStatus;
import handoff.application.Exception.ResourceNotFoundException;
import handoff.application.Repository.ActivityLogRepository;
import handoff.application.Repository.RequestRepository;
import handoff.application.Notification.ActivityRecordedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final RequestRepository requestRepository;
    private final TenantContext tenantContext;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Request request, ActivityAction action) {
        record(request, action, null, null);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Request request, ActivityAction action, RequestStatus from, RequestStatus to) {
        ActivityLog log = new ActivityLog();
        log.setRequest(request);
        log.setAction(action);
        log.setFromStatus(from);
        log.setToStatus(to);

        Optional<PortalPrincipal> portal = tenantContext.findPortalPrincipal();
        if (portal.isPresent()) {
            log.setActorType(ActorType.CLIENT);
            log.setPortalAccessId(portal.get().accessId());
        } else {
            AppUser user = tenantContext.findCurrentUser()
                    .orElseThrow(() -> new IllegalStateException("Ação sem autor autenticado"));
            log.setActorType(ActorType.USER);
            log.setActorUser(user);
        }
        activityLogRepository.save(log);
        eventPublisher.publishEvent(new ActivityRecordedEvent(request.getId(), action, log.getActorType(),
                log.getActorUser() == null ? null : log.getActorUser().getId(), from, to));
    }

    @Transactional(readOnly = true)
    public List<ActivityLogResponseDto> list(UUID requestId) {
        Request request = requestRepository.findByIdAndOrganizationId(requestId, tenantContext.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação"));
        return list(request);
    }

    @Transactional(readOnly = true)
    public List<ActivityLogResponseDto> listInProject(UUID projectId, UUID requestId) {
        Request request = requestRepository
                .findByIdAndOrganizationIdAndProjectId(requestId, tenantContext.getOrganizationId(), projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação"));
        return list(request);
    }

    private List<ActivityLogResponseDto> list(Request request) {
        String clientName = request.getProject().getClient().getName();
        return activityLogRepository
                .findAllByOrganizationIdAndRequestIdOrderByCreatedAtAsc(tenantContext.getOrganizationId(), request.getId())
                .stream()
                .map(log -> ActivityLogResponseDto.from(log, clientName))
                .toList();
    }
}
