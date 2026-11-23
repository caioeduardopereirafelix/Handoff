package handoff.application.Repository;

import handoff.application.Entity.Project;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    @EntityGraph(attributePaths = "client")
    Optional<Project> findByIdAndOrganizationId(UUID id, UUID organizationId);

    @EntityGraph(attributePaths = "client")
    List<Project> findAllByOrganizationIdOrderByNameAsc(UUID organizationId);

    @EntityGraph(attributePaths = "client")
    List<Project> findAllByOrganizationIdAndClientIdOrderByNameAsc(UUID organizationId, UUID clientId);

    boolean existsByOrganizationIdAndClientId(UUID organizationId, UUID clientId);
}
