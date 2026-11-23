package handoff.application.Controller;

import handoff.application.Config.PortalPrincipal;
import handoff.application.Dto.CreateRequestDto;
import handoff.application.Dto.PortalProjectDto;
import handoff.application.Dto.RequestResponseDto;
import handoff.application.Entity.ActorType;
import handoff.application.Entity.RequestStatus;
import handoff.application.Service.RequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/portal/{token}")
@RequiredArgsConstructor
public class PortalController {

    private final RequestService requestService;

    @GetMapping("/project")
    public PortalProjectDto project(@AuthenticationPrincipal PortalPrincipal portal) {
        return requestService.portalProject(portal.projectId());
    }

    @GetMapping("/requests")
    public List<RequestResponseDto> list(@AuthenticationPrincipal PortalPrincipal portal) {
        return requestService.listForProject(portal.projectId());
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public RequestResponseDto create(@AuthenticationPrincipal PortalPrincipal portal,
                                     @RequestBody @Valid CreateRequestDto request) {
        return requestService.create(portal.projectId(), request, ActorType.CLIENT);
    }

    @GetMapping("/requests/{id}")
    public RequestResponseDto get(@AuthenticationPrincipal PortalPrincipal portal, @PathVariable UUID id) {
        return requestService.getInProject(portal.projectId(), id);
    }

    @PostMapping("/requests/{id}/approve")
    public RequestResponseDto approve(@AuthenticationPrincipal PortalPrincipal portal, @PathVariable UUID id) {
        return requestService.changeStatusInProject(portal.projectId(), id, RequestStatus.DONE);
    }

    @PostMapping("/requests/{id}/request-changes")
    public RequestResponseDto requestChanges(@AuthenticationPrincipal PortalPrincipal portal, @PathVariable UUID id) {
        return requestService.changeStatusInProject(portal.projectId(), id, RequestStatus.CHANGES_REQUESTED);
    }
}
