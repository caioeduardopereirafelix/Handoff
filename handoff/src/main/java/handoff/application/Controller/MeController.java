package handoff.application.Controller;

import handoff.application.Dto.MeResponseDto;
import handoff.application.Entity.AppUser;
import handoff.application.Entity.Organization;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/me")
public class MeController {

    // O principal é o próprio AppUser, colocado no SecurityContext pelo
    // JwtAuthenticationFilter (já com a organização carregada).
    @GetMapping
    public MeResponseDto me(@AuthenticationPrincipal AppUser user) {
        Organization organization = user.getOrganization();
        return new MeResponseDto(user.getId(), user.getName(), user.getEmail(), user.getRole().name(),
                organization.getId(), organization.getName(), organization.getSlug());
    }
}
