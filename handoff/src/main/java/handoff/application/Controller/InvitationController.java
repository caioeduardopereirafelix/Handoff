package handoff.application.Controller;

import handoff.application.Config.TenantContext;
import handoff.application.Dto.AcceptInvitationDto;
import handoff.application.Dto.CreateInvitationDto;
import handoff.application.Dto.InvitationCreatedDto;
import handoff.application.Dto.InvitationPreviewDto;
import handoff.application.Dto.TokenResponseDto;
import handoff.application.Entity.AppUser;
import handoff.application.Service.InvitationService;
import handoff.application.Service.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;
    private final TenantContext tenantContext;
    private final RateLimiter rateLimiter;

    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/api/app/team/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public InvitationCreatedDto create(@RequestBody @Valid CreateInvitationDto request,
                                       @AuthenticationPrincipal AppUser user) {
        return invitationService.create(request, user);
    }

    @PreAuthorize("hasRole('OWNER')")
    @DeleteMapping("/api/app/team/invitations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable UUID id) {
        invitationService.revoke(id);
    }

    @GetMapping("/api/invitations/{token}")
    public InvitationPreviewDto preview(@PathVariable String token) {
        return tenantContext.acrossTenants(() -> invitationService.preview(token));
    }

    @PostMapping("/api/invitations/{token}/accept")
    public TokenResponseDto accept(@PathVariable String token,
                                   @RequestBody @Valid AcceptInvitationDto request,
                                   HttpServletRequest http) {
        rateLimiter.checkRegister(http.getRemoteAddr());
        return tenantContext.acrossTenants(() -> invitationService.accept(token, request));
    }
}
