package handoff.application.Service;

import handoff.application.Config.TenantContext;
import handoff.application.Dto.PendingInvitationDto;
import handoff.application.Dto.TeamMemberDto;
import handoff.application.Dto.TeamResponseDto;
import handoff.application.Dto.UpdateMemberDto;
import handoff.application.Entity.AppUser;
import handoff.application.Entity.Role;
import handoff.application.Exception.ResourceNotFoundException;
import handoff.application.Exception.TeamChangeNotAllowedException;
import handoff.application.Repository.AppUserRepository;
import handoff.application.Repository.InvitationRepository;
import handoff.application.Repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final AppUserRepository userRepository;
    private final InvitationRepository invitationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TenantContext tenantContext;
    private final Clock clock;

    @Transactional(readOnly = true)
    public TeamResponseDto list() {
        UUID organizationId = tenantContext.getOrganizationId();
        Instant now = clock.instant();
        return new TeamResponseDto(
                userRepository.findAllByOrganizationIdOrderByNameAsc(organizationId).stream()
                        .map(TeamMemberDto::from)
                        .toList(),
                invitationRepository
                        .findAllByOrganizationIdAndAcceptedAtIsNullAndRevokedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(organizationId, now)
                        .stream()
                        .map(PendingInvitationDto::from)
                        .toList());
    }

    @Transactional
    public TeamMemberDto update(UUID memberId, UpdateMemberDto request, AppUser currentUser) {
        UUID organizationId = tenantContext.getOrganizationId();
        AppUser member = userRepository.findByIdAndOrganizationId(memberId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Membro"));

        Role newRole = request.role() == null ? member.getRole() : request.role();
        boolean newActive = request.active() == null ? member.isActive() : request.active();
        boolean changes = newRole != member.getRole() || newActive != member.isActive();
        if (!changes) {
            return TeamMemberDto.from(member);
        }
        if (member.getId().equals(currentUser.getId())) {
            throw new TeamChangeNotAllowedException("Você não pode mudar o seu próprio papel nem desativar a sua conta");
        }
        boolean losesOwner = member.getRole() == Role.OWNER && member.isActive()
                && (newRole != Role.OWNER || !newActive);
        if (losesOwner && userRepository.countByOrganizationIdAndRoleAndActiveTrue(organizationId, Role.OWNER) <= 1) {
            throw new TeamChangeNotAllowedException("A equipe precisa de pelo menos um dono ativo");
        }

        member.setRole(newRole);
        member.setActive(newActive);
        userRepository.saveAndFlush(member);
        if (!newActive) {
            refreshTokenRepository.revokeAllOfUser(member.getId(), clock.instant());
        }
        return TeamMemberDto.from(member);
    }
}
