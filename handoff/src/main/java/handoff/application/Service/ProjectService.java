package handoff.application.Service;

import handoff.application.Config.TenantContext;
import handoff.application.Dto.ProjectRequestDto;
import handoff.application.Dto.ProjectResponseDto;
import handoff.application.Dto.ProjectUpdateDto;
import handoff.application.Entity.Project;
import handoff.application.Exception.ResourceNotFoundException;
import handoff.application.Repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ClientService clientService;
    private final TenantContext tenantContext;

    @Transactional(readOnly = true)
    public List<ProjectResponseDto> list(UUID clientId) {
        UUID orgId = tenantContext.getOrganizationId();
        List<Project> projects = clientId == null
                ? projectRepository.findAllByOrganizationIdOrderByNameAsc(orgId)
                : projectRepository.findAllByOrganizationIdAndClientIdOrderByNameAsc(orgId, clientId);
        return projects.stream().map(ProjectResponseDto::from).toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponseDto get(UUID id) {
        return ProjectResponseDto.from(find(id));
    }

    @Transactional
    public ProjectResponseDto create(ProjectRequestDto request) {
        Project project = new Project();
        project.setClient(clientService.find(request.clientId()));
        project.setName(request.name().trim());
        return ProjectResponseDto.from(projectRepository.saveAndFlush(project));
    }

    @Transactional
    public ProjectResponseDto update(UUID id, ProjectUpdateDto request) {
        Project project = find(id);
        project.setName(request.name().trim());
        project.setStatus(request.status());
        return ProjectResponseDto.from(projectRepository.saveAndFlush(project));
    }

    @Transactional
    public void delete(UUID id) {
        projectRepository.delete(find(id));
    }

    Project find(UUID id) {
        return projectRepository.findByIdAndOrganizationId(id, tenantContext.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Projeto"));
    }
}
