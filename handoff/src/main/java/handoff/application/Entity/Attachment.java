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
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @TenantId
    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false, updatable = false)
    private Request request;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", updatable = false)
    private Comment comment;

    @Column(name = "storage_key", nullable = false, unique = true, length = 300, updatable = false)
    private String storageKey;

    @Column(name = "original_name", nullable = false, length = 255, updatable = false)
    private String originalName;

    @Column(name = "content_type", nullable = false, length = 120, updatable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false, updatable = false)
    private long sizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AttachmentStatus status = AttachmentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "uploaded_by_type", nullable = false, length = 10, updatable = false)
    private ActorType uploadedByType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_user_id", updatable = false)
    private AppUser uploadedByUser;

    @Column(name = "portal_access_id", updatable = false)
    private UUID portalAccessId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}
