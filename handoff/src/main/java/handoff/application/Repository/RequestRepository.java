package handoff.application.Repository;

import handoff.application.Entity.Request;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RequestRepository extends JpaRepository<Request, UUID> {

    @EntityGraph(attributePaths = {"project", "assignee"})
    Optional<Request> findByIdAndOrganizationId(UUID id, UUID organizationId);

    @EntityGraph(attributePaths = {"project", "assignee"})
    Optional<Request> findByIdAndOrganizationIdAndProjectId(UUID id, UUID organizationId, UUID projectId);

    @EntityGraph(attributePaths = {"project", "assignee"})
    List<Request> findAllByOrganizationIdAndProjectIdOrderByNumberDesc(UUID organizationId, UUID projectId);
}
