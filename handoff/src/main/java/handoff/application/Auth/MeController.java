package handoff.application.Auth;

import handoff.application.Entity.AppUser;
import handoff.application.Repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/app/me")
@RequiredArgsConstructor
public class MeController {

    private final AppUserRepository userRepository;

    public record MeResponse(UUID id, String name, String email, String role,
                             UUID organizationId, String organizationName, String organizationSlug) {
    }

    @GetMapping
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        UUID orgId = UUID.fromString(jwt.getClaimAsString(AccessTokenService.CLAIM_ORG_ID));
        AppUser user = userRepository.findByIdAndOrganizationId(userId, orgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new MeResponse(user.getId(), user.getName(), user.getEmail(), user.getRole().name(),
                user.getOrganization().getId(), user.getOrganization().getName(), user.getOrganization().getSlug());
    }
}
