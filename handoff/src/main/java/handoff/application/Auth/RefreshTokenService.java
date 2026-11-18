package handoff.application.Auth;

import handoff.application.Config.JwtProperties;
import handoff.application.Entity.AppUser;
import handoff.application.Entity.RefreshToken;
import handoff.application.Repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

// Refresh token opaco com rotação: cada uso gera um novo token e revoga o
// anterior. Reapresentar um token já revogado indica roubo, e a família inteira
// (todos os tokens daquele login) é revogada.
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final JwtProperties properties;
    private final Clock clock;

    public record Rotation(AppUser user, String refreshToken) {
    }

    @Transactional
    public String issueNewFamily(AppUser user) {
        return issue(user, UUID.randomUUID()).raw();
    }

    // noRollbackFor: a revogação da família em caso de reuso precisa ser
    // gravada mesmo com a exceção sendo lançada.
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public Rotation rotate(String rawToken) {
        Instant now = clock.instant();
        RefreshToken current = repository.findForUpdateByTokenHash(TokenHasher.sha256Hex(rawToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (current.getRevokedAt() != null) {
            repository.revokeFamily(current.getFamilyId(), now);
            throw new InvalidRefreshTokenException();
        }
        AppUser user = current.getUser();
        if (!current.isUsable(now) || !user.isActive()) {
            throw new InvalidRefreshTokenException();
        }

        Issued next = issue(user, current.getFamilyId());
        current.setRevokedAt(now);
        current.setReplacedBy(next.entity().getId());
        return new Rotation(user, next.raw());
    }

    // Logout encerra a sessão inteira daquele login. Token desconhecido é
    // ignorado, para que o logout seja idempotente.
    @Transactional
    public void revokeFamilyOf(String rawToken) {
        repository.findForUpdateByTokenHash(TokenHasher.sha256Hex(rawToken))
                .ifPresent(token -> repository.revokeFamily(token.getFamilyId(), clock.instant()));
    }

    private record Issued(RefreshToken entity, String raw) {
    }

    private Issued issue(AppUser user, UUID familyId) {
        String raw = TokenHasher.newToken();
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(TokenHasher.sha256Hex(raw));
        token.setFamilyId(familyId);
        token.setExpiresAt(clock.instant().plus(properties.refreshTokenTtl()));
        return new Issued(repository.saveAndFlush(token), raw);
    }
}
