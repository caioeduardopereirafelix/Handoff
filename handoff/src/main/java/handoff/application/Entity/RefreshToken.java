package handoff.application.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private AppUser user;

    // SHA-256 do token em hex; o token puro só existe na resposta ao cliente
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    // Agrupa os tokens de um mesmo login: se um token revogado for reutilizado,
    // a família inteira é revogada
    @Column(name = "family_id", nullable = false, updatable = false)
    private UUID familyId;

    @Column(name = "replaced_by")
    private UUID replacedBy;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public boolean isUsable(Instant now) {
        return revokedAt == null && now.isBefore(expiresAt);
    }
}
