package handoff.application.Notification;

import handoff.application.Config.TenantContext;
import handoff.application.Entity.ActivityAction;
import handoff.application.Entity.ActorType;
import handoff.application.Entity.AppUser;
import handoff.application.Entity.Project;
import handoff.application.Entity.Request;
import handoff.application.Entity.RequestStatus;
import handoff.application.Entity.Role;
import handoff.application.Repository.AppUserRepository;
import handoff.application.Repository.RequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ActivityNotificationListener {

    private final RequestRepository requestRepository;
    private final AppUserRepository userRepository;
    private final TenantContext tenantContext;
    private final NotificationPublisher publisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onActivity(ActivityRecordedEvent event) {
        if (event.action() == ActivityAction.ASSIGNMENT_CHANGED) {
            return;
        }
        Optional<Request> found = requestRepository.findByIdAndOrganizationId(event.requestId(), tenantContext.getOrganizationId());
        if (found.isEmpty()) {
            return;
        }
        Request request = found.get();
        Project project = request.getProject();
        String clientName = project.getClient().getName();
        String code = "#%03d".formatted(request.getNumber());

        if (event.actorType() == ActorType.USER) {
            String clientEmail = project.getClient().getEmail();
            if (!StringUtils.hasText(clientEmail)) {
                return;
            }
            String actorName = event.actorUserId() == null ? "A equipe" : userRepository.findById(event.actorUserId())
                    .map(AppUser::getName).orElse("A equipe");
            String what = describeForClient(event);
            publisher.publish(new EmailMessage(
                    List.of(clientEmail),
                    "[%s] %s %s: %s".formatted(project.getName(), code, request.getTitle(), what),
                    """
                    Olá, %s.

                    %s %s.

                    Solicitação %s – %s
                    Status atual: %s

                    Abra o portal pelo link que você recebeu para acompanhar e responder.
                    """.formatted(clientName, actorName, what, code, request.getTitle(), label(request.getStatus()))));
            return;
        }

        List<String> recipients = developersToNotify(request);
        if (recipients.isEmpty()) {
            return;
        }
        String what = describeForDevelopers(event);
        publisher.publish(new EmailMessage(
                recipients,
                "[%s] %s %s: %s".formatted(project.getName(), code, request.getTitle(), what),
                """
                %s %s.

                Projeto: %s
                Solicitação %s – %s
                Status atual: %s

                Abra o painel do Handoff para responder.
                """.formatted(clientName, what, project.getName(), code, request.getTitle(), label(request.getStatus()))));
    }

    private List<String> developersToNotify(Request request) {
        AppUser assignee = request.getAssignee();
        if (assignee != null && assignee.isEnabled()) {
            return List.of(assignee.getEmail());
        }
        return userRepository.findAllByOrganizationIdAndRoleAndActiveTrue(request.getOrganizationId(), Role.OWNER)
                .stream()
                .map(AppUser::getEmail)
                .toList();
    }

    private static String describeForClient(ActivityRecordedEvent event) {
        return switch (event.action()) {
            case REQUEST_CREATED -> "abriu uma nova solicitação";
            case COMMENT_ADDED -> "comentou na solicitação";
            case ATTACHMENT_ADDED -> "anexou um arquivo";
            case STATUS_CHANGED -> event.toStatus() == RequestStatus.AWAITING_APPROVAL
                    ? "entregou a solicitação, que aguarda a sua aprovação"
                    : "mudou o status para " + label(event.toStatus()).toLowerCase();
            case ASSIGNMENT_CHANGED -> "alterou o responsável";
        };
    }

    private static String describeForDevelopers(ActivityRecordedEvent event) {
        return switch (event.action()) {
            case REQUEST_CREATED -> "abriu uma nova solicitação";
            case COMMENT_ADDED -> "comentou na solicitação";
            case ATTACHMENT_ADDED -> "anexou um arquivo";
            case STATUS_CHANGED -> switch (event.toStatus()) {
                case DONE -> "aprovou a entrega";
                case CHANGES_REQUESTED -> "pediu ajustes na entrega";
                default -> "mudou o status para " + label(event.toStatus()).toLowerCase();
            };
            case ASSIGNMENT_CHANGED -> "alterou o responsável";
        };
    }

    private static String label(RequestStatus status) {
        return switch (status) {
            case OPEN -> "Aberta";
            case IN_PROGRESS -> "Em andamento";
            case AWAITING_APPROVAL -> "Aguardando aprovação";
            case CHANGES_REQUESTED -> "Ajuste solicitado";
            case DONE -> "Concluída";
            case CANCELED -> "Cancelada";
        };
    }
}
