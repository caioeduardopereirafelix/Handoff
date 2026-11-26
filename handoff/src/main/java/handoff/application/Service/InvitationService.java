package handoff.application.Service;

import handoff.application.Config.NotificationProperties;
import handoff.application.Config.TenantContext;
import handoff.application.Dto.AcceptInvitationDto;
import handoff.application.Dto.CreateInvitationDto;
import handoff.application.Dto.InvitationCreatedDto;
import handoff.application.Dto.InvitationPreviewDto;
import handoff.application.Dto.TokenResponseDto;
import handoff.application.Entity.AppUser;
import handoff.application.Entity.AuthProvider;
import handoff.application.Entity.Invitation;
import handoff.application.Entity.Organization;
import handoff.application.Entity.Role;
import handoff.application.Exception.EmailAlreadyUsedException;
import handoff.application.Exception.ResourceNotFoundException;
import handoff.application.Notification.EmailMessage;
import handoff.application.Notification.EmailRequestedEvent;
import handoff.application.Repository.AppUserRepository;
import handoff.application.Repository.InvitationRepository;
import handoff.application.Repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvitationService {

    static final Duration EXPIRATION = Duration.ofDays(7);

    private final InvitationRepository invitationRepository;
    private final AppUserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;
    private final TenantContext tenantContext;
    private final NotificationProperties notificationProperties;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Transactional
    public InvitationCreatedDto create(CreateInvitationDto request, AppUser inviter) {
        String email = AuthService.normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyUsedException();
        }
        Instant now = clock.instant();
        invitationRepository
                .findAllByOrganizationIdAndEmailAndAcceptedAtIsNullAndRevokedAtIsNull(tenantContext.getOrganizationId(), email)
                .forEach(previous -> previous.setRevokedAt(now));

        String token = TokenHasher.newToken();
        Invitation invitation = new Invitation();
        invitation.setEmail(email);
        invitation.setRole(request.role() == null ? Role.MEMBER : request.role());
        invitation.setTokenHash(TokenHasher.sha256Hex(token));
        invitation.setInvitedBy(inviter);
        invitation.setExpiresAt(now.plus(EXPIRATION));
        invitationRepository.saveAndFlush(invitation);

        sendInvitation(invitation, token, inviter);
        return new InvitationCreatedDto(invitation.getId(), email, invitation.getRole(), token,
                invitation.getExpiresAt(), notificationProperties.enabled());
    }

    @Transactional
    public void revoke(UUID invitationId) {
        Invitation invitation = invitationRepository
                .findByIdAndOrganizationId(invitationId, tenantContext.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Convite"));
        if (invitation.getAcceptedAt() == null && invitation.getRevokedAt() == null) {
            invitation.setRevokedAt(clock.instant());
        }
    }

    @Transactional(readOnly = true)
    public InvitationPreviewDto preview(String token) {
        Invitation invitation = findPending(token);
        return new InvitationPreviewDto(organizationOf(invitation).getName(), inviterName(invitation),
                invitation.getEmail(), invitation.getExpiresAt());
    }

    @Transactional
    public TokenResponseDto accept(String token, AcceptInvitationDto request) {
        Invitation invitation = findPending(token);
        if (userRepository.existsByEmail(invitation.getEmail())) {
            throw new EmailAlreadyUsedException();
        }

        AppUser user = new AppUser();
        user.setOrganization(organizationOf(invitation));
        user.setName(request.name().trim());
        user.setEmail(invitation.getEmail());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(invitation.getRole());
        user.setAuthProvider(AuthProvider.LOCAL);
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new EmailAlreadyUsedException();
        }
        invitation.setAcceptedAt(clock.instant());
        return authService.issueTokens(user);
    }

    private Invitation findPending(String token) {
        Instant now = clock.instant();
        return invitationRepository.findByTokenHash(TokenHasher.sha256Hex(token))
                .filter(invitation -> invitation.isPending(now))
                .orElseThrow(() -> new ResourceNotFoundException("Convite"));
    }

    private Organization organizationOf(Invitation invitation) {
        return organizationRepository.findById(invitation.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Convite"));
    }

    private static String inviterName(Invitation invitation) {
        return invitation.getInvitedBy() == null ? "A equipe" : invitation.getInvitedBy().getName();
    }

    private void sendInvitation(Invitation invitation, String token, AppUser inviter) {
        String organizationName = organizationOf(invitation).getName();
        String link = notificationProperties.portalBaseUrl().replaceAll("/+$", "") + "/convite/" + token;
        String validUntil = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.of("America/Sao_Paulo"))
                .format(invitation.getExpiresAt());
        eventPublisher.publishEvent(new EmailRequestedEvent(new EmailMessage(
                List.of(invitation.getEmail()),
                "%s convidou você para a equipe %s no Handoff".formatted(inviter.getName(), organizationName),
                """
                Olá!

                %s convidou você para a equipe %s no Handoff, onde vocês acompanham as solicitações dos clientes.

                Para entrar, abra o link abaixo e crie a sua senha:

                %s

                O convite vale até %s e só pode ser usado uma vez. Se você não esperava este convite, ignore este e-mail.
                """.formatted(inviter.getName(), organizationName, link, validUntil))));
    }
}
