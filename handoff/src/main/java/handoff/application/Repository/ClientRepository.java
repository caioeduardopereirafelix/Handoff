package handoff.application.Repository;

import handoff.application.Entity.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ClientRepository extends JpaRepository<Client, UUID> {

    Optional<Client> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Page<Client> findAllByOrganizationId(UUID organizationId, Pageable pageable);
}
