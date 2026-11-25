package handoff.application.Service;

import handoff.application.Config.TenantContext;
import handoff.application.Dto.AssignRequestDto;
import handoff.application.Dto.CreateRequestDto;
import handoff.application.Dto.PageResponseDto;
import handoff.application.Dto.PortalProjectDto;
import handoff.application.Dto.RequestResponseDto;
import handoff.application.Entity.ActivityAction;
import handoff.application.Entity.ActorType;
import handoff.application.Entity.AppUser;
import handoff.application.Entity.Project;
import handoff.application.Entity.Request;
import handoff.application.Entity.RequestPriority;
import handoff.application.Entity.RequestStatus;
import handoff.application.Exception.ResourceNotFoundException;
import handoff.application.Repository.AppUserRepository;
import handoff.application.Repository.ProjectRepository;
import handoff.application.Repository.RequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RequestService {

    private final RequestRepository requestRepository;
    private final ProjectRepository projectRepository;
    private final AppUserRepository userRepository;
    private final ProjectService projectService;
    private final TenantContext tenantContext;
    private final ActivityLogService activityLogService;

    @Transactional(readOnly = true)
    public PageResponseDto<RequestResponseDto> listForProject(UUID projectId, Set<RequestStatus> statuses, int page, int size) {
        projectService.find(projectId);
        UUID orgId = tenantContext.getOrganizationId();
        Pageable pageable = Pagination.of(page, size, Sort.by(Sort.Direction.DESC, "number"));
        Page<Request> requests = statuses == null || statuses.isEmpty()
                ? requestRepository.findAllByOrganizationIdAndProjectId(orgId, projectId, pageable)
                : requestRepository.findAllByOrganizationIdAndProjectIdAndStatusIn(orgId, projectId, statuses, pageable);
        return PageResponseDto.from(requests, RequestResponseDto::from);
    }

    @Transactional(readOnly = true)
    public RequestResponseDto get(UUID id) {
        return RequestResponseDto.from(find(id));
    }

    @Transactional(readOnly = true)
    public RequestResponseDto getInProject(UUID projectId, UUID id) {
        return RequestResponseDto.from(findInProject(projectId, id));
    }

    @Transactional
    public RequestResponseDto create(UUID projectId, CreateRequestDto dto, ActorType actor) {
        Project project = projectRepository.findForUpdate(projectId, tenantContext.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Projeto"));
        int number = project.getNextRequestNumber();
        project.setNextRequestNumber(number + 1);

        Request request = new Request();
        request.setProject(project);
        request.setNumber(number);
        request.setTitle(dto.title().trim());
        request.setDescription(dto.description() == null || dto.description().isBlank() ? null : dto.description().trim());
        request.setPriority(dto.priority() == null ? RequestPriority.MEDIUM : dto.priority());
        request.setCreatedByType(actor);
        Request saved = requestRepository.saveAndFlush(request);
        activityLogService.record(saved, ActivityAction.REQUEST_CREATED, null, RequestStatus.OPEN);
        return RequestResponseDto.from(saved);
    }

    @Transactional
    public RequestResponseDto changeStatus(UUID id, RequestStatus target) {
        Request request = find(id);
        RequestStatus from = request.getStatus();
        request.transitionTo(target, ActorType.USER);
        activityLogService.record(request, ActivityAction.STATUS_CHANGED, from, target);
        return RequestResponseDto.from(requestRepository.saveAndFlush(request));
    }

    @Transactional
    public RequestResponseDto changeStatusInProject(UUID projectId, UUID id, RequestStatus target) {
        Request request = findInProject(projectId, id);
        RequestStatus from = request.getStatus();
        request.transitionTo(target, ActorType.CLIENT);
        activityLogService.record(request, ActivityAction.STATUS_CHANGED, from, target);
        return RequestResponseDto.from(requestRepository.saveAndFlush(request));
    }

    @Transactional
    public RequestResponseDto assign(UUID id, AssignRequestDto dto) {
        Request request = find(id);
        AppUser assignee = dto.assigneeId() == null
                ? null
                : userRepository.findByIdAndOrganizationId(dto.assigneeId(), tenantContext.getOrganizationId())
                        .orElseThrow(() -> new ResourceNotFoundException("Responsável"));
        request.setAssignee(assignee);
        request.setDueDate(dto.dueDate());
        activityLogService.record(request, ActivityAction.ASSIGNMENT_CHANGED);
        return RequestResponseDto.from(requestRepository.saveAndFlush(request));
    }

    @Transactional(readOnly = true)
    public PortalProjectDto portalProject(UUID projectId) {
        Project project = projectService.find(projectId);
        return new PortalProjectDto(project.getId(), project.getName(), project.getClient().getName(), project.getStatus());
    }

    private Request find(UUID id) {
        return requestRepository.findByIdAndOrganizationId(id, tenantContext.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação"));
    }

    private Request findInProject(UUID projectId, UUID id) {
        return requestRepository.findByIdAndOrganizationIdAndProjectId(id, tenantContext.getOrganizationId(), projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação"));
    }
}
