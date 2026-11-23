package handoff.application.Controller;

import handoff.application.Dto.ProjectRequestDto;
import handoff.application.Dto.ProjectResponseDto;
import handoff.application.Dto.ProjectUpdateDto;
import handoff.application.Service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/app/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public List<ProjectResponseDto> list(@RequestParam(required = false) UUID clientId) {
        return projectService.list(clientId);
    }

    @GetMapping("/{id}")
    public ProjectResponseDto get(@PathVariable UUID id) {
        return projectService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponseDto create(@RequestBody @Valid ProjectRequestDto request) {
        return projectService.create(request);
    }

    @PutMapping("/{id}")
    public ProjectResponseDto update(@PathVariable UUID id, @RequestBody @Valid ProjectUpdateDto request) {
        return projectService.update(id, request);
    }

    @PreAuthorize("hasRole('OWNER')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        projectService.delete(id);
    }
}
