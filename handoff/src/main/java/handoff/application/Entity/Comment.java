package handoff.application.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @TenantId
    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false, updatable = false)
    private Request request;

    @Enumerated(EnumType.STRING)
    @Column(name = "author_type", nullable = false, length = 10, updatable = false)
    private ActorType authorType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_user_id", updatable = false)
    private AppUser authorUser;

    @Column(name = "portal_access_id", updatable = false)
    private UUID portalAccessId;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
