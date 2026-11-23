package handoff.application.Repository;

import handoff.application.Entity.PortalAccess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PortalAccessRepository extends JpaRepository<PortalAccess, UUID> {

    Optional<PortalAccess> findByTokenHash(String tokenHash);

    Optional<PortalAccess> findByIdAndOrganizationIdAndProjectId(UUID id, UUID organizationId, UUID projectId);

    List<PortalAccess> findAllByOrganizationIdAndProjectIdOrderByCreatedAtDesc(UUID organizationId, UUID projectId);
}
