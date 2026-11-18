package handoff.application.Auth;

import handoff.application.Auth.AuthDtos.AuthResponse;
import handoff.application.Auth.AuthDtos.LoginRequest;
import handoff.application.Auth.AuthDtos.SignupRequest;
import handoff.application.Entity.AppUser;
import handoff.application.Entity.AuthProvider;
import handoff.application.Entity.Organization;
import handoff.application.Entity.Role;
import handoff.application.Repository.AppUserRepository;
import handoff.application.Repository.OrganizationRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.HexFormat;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AuthService {

    private final OrganizationRepository organizationRepository;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenService accessTokenService;
    private final RefreshTokenService refreshTokenService;

    // Usado quando o e-mail não existe, para o login levar o mesmo tempo
    // e não revelar quais e-mails estão cadastrados.
    private final String dummyPasswordHash;

    public AuthService(OrganizationRepository organizationRepository,
                       AppUserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AccessTokenService accessTokenService,
                       RefreshTokenService refreshTokenService) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.accessTokenService = accessTokenService;
        this.refreshTokenService = refreshTokenService;
        this.dummyPasswordHash = passwordEncoder.encode("handoff-dummy-password");
    }

    // O cadastro cria a organização e o usuário OWNER dela.
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyUsedException();
        }

        Organization organization = organizationRepository.save(Organization.builder()
                .name(request.organizationName().trim())
                .slug(uniqueSlug(request.organizationName()))
                .build());

        AppUser user = new AppUser();
        user.setOrganization(organization);
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.OWNER);
        user.setAuthProvider(AuthProvider.LOCAL);
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // dois cadastros simultâneos com o mesmo e-mail
            throw new EmailAlreadyUsedException();
        }

        return tokensFor(user, refreshTokenService.issueNewFamily(user));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        AppUser user = userRepository.findByEmail(normalizeEmail(request.email())).orElse(null);

        boolean passwordMatches = passwordEncoder.matches(request.password(),
                user != null && user.getPassword() != null ? user.getPassword() : dummyPasswordHash);

        if (user == null || user.getAuthProvider() != AuthProvider.LOCAL || !user.isActive() || !passwordMatches) {
            throw new InvalidCredentialsException();
        }
        return tokensFor(user, refreshTokenService.issueNewFamily(user));
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public AuthResponse refresh(String refreshToken) {
        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(refreshToken);
        return tokensFor(rotation.user(), rotation.refreshToken());
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revokeFamilyOf(refreshToken);
    }

    private AuthResponse tokensFor(AppUser user, String refreshToken) {
        return new AuthResponse(accessTokenService.issue(user), refreshToken, "Bearer",
                accessTokenService.accessTokenTtlSeconds());
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String uniqueSlug(String organizationName) {
        String base = slugify(organizationName);
        String slug = base;
        while (organizationRepository.existsBySlug(slug)) {
            slug = base + "-" + HexFormat.of().toHexDigits(ThreadLocalRandom.current().nextInt()).substring(0, 6);
        }
        return slug;
    }

    static String slugify(String text) {
        String slug = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        if (slug.length() > 60) {
            slug = slug.substring(0, 60).replaceAll("-+$", "");
        }
        return slug.isEmpty() ? "org" : slug;
    }
}
