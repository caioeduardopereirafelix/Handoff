package handoff.application.Service;

import handoff.application.Config.PortalPrincipal;
import handoff.application.Config.TenantContext;
import handoff.application.Dto.CreatePortalAccessDto;
import handoff.application.Dto.PortalAccessCreatedDto;
import handoff.application.Dto.PortalAccessResponseDto;
import handoff.application.Entity.AppUser;
import handoff.application.Entity.PortalAccess;
import handoff.application.Exception.ResourceNotFoundException;
import handoff.application.Repository.PortalAccessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PortalAccessService {

    private static final int DEFAULT_EXPIRATION_DAYS = 90;

    private final PortalAccessRepository portalAccessRepository;
    private final ProjectService projectService;
    private final TenantContext tenantContext;
    private final Clock clock;

    @Transactional
    public PortalAccessCreatedDto create(UUID projectId, CreatePortalAccessDto request, AppUser createdBy) {
        int days = request == null || request.expiresInDays() == null
                ? DEFAULT_EXPIRATION_DAYS
                : request.expiresInDays();
        String token = TokenHasher.newToken();

        PortalAccess access = new PortalAccess();
        access.setProject(projectService.find(projectId));
        access.setTokenHash(TokenHasher.sha256Hex(token));
        access.setCreatedBy(createdBy.getId());
        access.setExpiresAt(clock.instant().plus(Duration.ofDays(days)));
        portalAccessRepository.saveAndFlush(access);

        return new PortalAccessCreatedDto(access.getId(), token, "/api/portal/" + token, access.getExpiresAt());
    }

    @Transactional(readOnly = true)
    public List<PortalAccessResponseDto> list(UUID projectId) {
        projectService.find(projectId);
        Instant now = clock.instant();
        return portalAccessRepository
                .findAllByOrganizationIdAndProjectIdOrderByCreatedAtDesc(tenantContext.getOrganizationId(), projectId)
                .stream()
                .map(access -> PortalAccessResponseDto.from(access, now))
                .toList();
    }

    @Transactional
    public void revoke(UUID projectId, UUID accessId) {
        PortalAccess access = portalAccessRepository
                .findByIdAndOrganizationIdAndProjectId(accessId, tenantContext.getOrganizationId(), projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Link do portal"));
        if (access.getRevokedAt() == null) {
            access.setRevokedAt(clock.instant());
        }
    }

    @Transactional
    public Optional<PortalPrincipal> authenticate(String rawToken, String ip) {
        Instant now = clock.instant();
        return portalAccessRepository.findByTokenHash(TokenHasher.sha256Hex(rawToken))
                .filter(access -> access.isActive(now))
                .map(access -> {
                    access.setLastUsedAt(now);
                    access.setLastUsedIp(ip);
                    return new PortalPrincipal(access.getId(), access.getOrganizationId(), access.getProject().getId());
                });
    }
}
