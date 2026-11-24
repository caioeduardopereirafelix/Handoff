package handoff.application.Repository;

import handoff.application.Entity.ActivityLog;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, UUID> {

    @EntityGraph(attributePaths = "actorUser")
    List<ActivityLog> findAllByOrganizationIdAndRequestIdOrderByCreatedAtAsc(UUID organizationId, UUID requestId);
}
