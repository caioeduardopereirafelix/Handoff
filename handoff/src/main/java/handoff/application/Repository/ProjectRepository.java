package handoff.application.Repository;

import handoff.application.Entity.Project;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    @EntityGraph(attributePaths = "client")
    Optional<Project> findByIdAndOrganizationId(UUID id, UUID organizationId);

    @EntityGraph(attributePaths = "client")
    Page<Project> findAllByOrganizationId(UUID organizationId, Pageable pageable);

    @EntityGraph(attributePaths = "client")
    Page<Project> findAllByOrganizationIdAndClientId(UUID organizationId, UUID clientId, Pageable pageable);

    boolean existsByOrganizationIdAndClientId(UUID organizationId, UUID clientId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Project p where p.id = :id and p.organizationId = :organizationId")
    Optional<Project> findForUpdate(@Param("id") UUID id, @Param("organizationId") UUID organizationId);
}
