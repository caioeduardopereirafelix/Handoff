package handoff.application.Controller;

import handoff.application.Dto.CreatePortalAccessDto;
import handoff.application.Dto.PortalAccessCreatedDto;
import handoff.application.Dto.PortalAccessResponseDto;
import handoff.application.Entity.AppUser;
import handoff.application.Service.PortalAccessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/app/projects/{projectId}/portal-access")
@RequiredArgsConstructor
public class PortalAccessController {

    private final PortalAccessService portalAccessService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PortalAccessCreatedDto create(@PathVariable UUID projectId,
                                         @RequestBody(required = false) @Valid CreatePortalAccessDto request,
                                         @AuthenticationPrincipal AppUser user) {
        return portalAccessService.create(projectId, request, user);
    }

    @GetMapping
    public List<PortalAccessResponseDto> list(@PathVariable UUID projectId) {
        return portalAccessService.list(projectId);
    }

    @DeleteMapping("/{accessId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable UUID projectId, @PathVariable UUID accessId) {
        portalAccessService.revoke(projectId, accessId);
    }
}
