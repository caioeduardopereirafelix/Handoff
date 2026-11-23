package handoff.application.Config;

import handoff.application.Entity.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

// Gera e valida o JWT de acesso (jjwt, HS256).
@Component
public class TokenProvider {

    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_ORG_ID = "orgId";
    public static final String CLAIM_ROLE = "role";

    private final SecretKey signingKey;
    private final Duration expiration;


    public TokenProvider(@Value("${jwt.key}") String key,
                         @Value("${jwt.expiration}") Duration expiration) {
        this.signingKey = Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    public String generateToken(AppUser user) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expiration.toMillis());

        return Jwts.builder()
                .subject(user.getEmail())
                .claim(CLAIM_USER_ID, user.getId().toString())
                .claim(CLAIM_ORG_ID, user.getOrganization().getId().toString())
                .claim(CLAIM_ROLE, user.getRole().name())
                .issuedAt(now)
                .expiration(expirationDate)
                .signWith(signingKey)
                .compact();
    }

    public boolean isValid(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    public long getExpirationMillis() {
        return expiration.toMillis();
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
