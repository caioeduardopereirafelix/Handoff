package handoff.application.Entity;

import handoff.application.Exception.ForbiddenTransitionException;
import handoff.application.Exception.InvalidTransitionException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.TenantId;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Request {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @TenantId
    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, updatable = false)
    private Project project;

    @Column(nullable = false, updatable = false)
    private int number;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RequestPriority priority = RequestPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RequestStatus status = RequestStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "created_by_type", nullable = false, length = 10, updatable = false)
    private ActorType createdByType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private AppUser assignee;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Version
    @Column(nullable = false)
    private long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void transitionTo(RequestStatus target, ActorType actor) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidTransitionException(status, target);
        }
        if (status.actorAllowedTo(target).orElseThrow() != actor) {
            throw new ForbiddenTransitionException(status, target);
        }
        status = target;
    }
}
