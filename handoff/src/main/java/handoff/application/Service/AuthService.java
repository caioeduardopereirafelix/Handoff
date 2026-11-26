package handoff.application.Service;

import handoff.application.Config.TokenProvider;
import handoff.application.Dto.LoginRequestDto;
import handoff.application.Dto.RegisterRequestDto;
import handoff.application.Dto.TokenResponseDto;
import handoff.application.Entity.AppUser;
import handoff.application.Entity.AuthProvider;
import handoff.application.Entity.Organization;
import handoff.application.Entity.Role;
import handoff.application.Exception.EmailAlreadyUsedException;
import handoff.application.Exception.InvalidCredentialsException;
import handoff.application.Exception.InvalidRefreshTokenException;
import handoff.application.Repository.AppUserRepository;
import handoff.application.Validation.MaxBytesValidator;
import handoff.application.Repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.HexFormat;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_PASSWORD_BYTES = 72;

    private final OrganizationRepository organizationRepository;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public TokenResponseDto register(RegisterRequestDto request) {
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
            throw new EmailAlreadyUsedException();
        }

        return tokensFor(user, refreshTokenService.issueNewFamily(user));
    }

    @Transactional
    public TokenResponseDto login(LoginRequestDto request) {
        if (!MaxBytesValidator.fits(request.password(), MAX_PASSWORD_BYTES)) {
            throw new InvalidCredentialsException();
        }
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizeEmail(request.email()), request.password()));
        } catch (AuthenticationException e) {
            throw new InvalidCredentialsException();
        }

        AppUser user = (AppUser) authentication.getPrincipal();
        return tokensFor(user, refreshTokenService.issueNewFamily(user));
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public TokenResponseDto refresh(String refreshToken) {
        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(refreshToken);
        return tokensFor(rotation.user(), rotation.refreshToken());
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revokeFamilyOf(refreshToken);
    }

    TokenResponseDto issueTokens(AppUser user) {
        return tokensFor(user, refreshTokenService.issueNewFamily(user));
    }

    private TokenResponseDto tokensFor(AppUser user, String refreshToken) {
        return new TokenResponseDto(tokenProvider.generateToken(user), "Bearer",
                tokenProvider.getExpirationMillis(), refreshToken);
    }

    static String normalizeEmail(String email) {
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
