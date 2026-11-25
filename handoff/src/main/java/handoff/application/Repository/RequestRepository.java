package handoff.application.Repository;

import handoff.application.Entity.Request;
import handoff.application.Entity.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface RequestRepository extends JpaRepository<Request, UUID> {

    @EntityGraph(attributePaths = {"project", "assignee"})
    Optional<Request> findByIdAndOrganizationId(UUID id, UUID organizationId);

    @EntityGraph(attributePaths = {"project", "assignee"})
    Optional<Request> findByIdAndOrganizationIdAndProjectId(UUID id, UUID organizationId, UUID projectId);

    @EntityGraph(attributePaths = {"project", "assignee"})
    Page<Request> findAllByOrganizationIdAndProjectId(UUID organizationId, UUID projectId, Pageable pageable);

    @EntityGraph(attributePaths = {"project", "assignee"})
    Page<Request> findAllByOrganizationIdAndProjectIdAndStatusIn(UUID organizationId, UUID projectId,
                                                                 Collection<RequestStatus> statuses, Pageable pageable);
}
