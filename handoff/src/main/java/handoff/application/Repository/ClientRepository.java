package handoff.application.Repository;

import handoff.application.Entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Primeira camada do isolamento: toda consulta recebe o orgId. Não use o
// findById herdado do JpaRepository em entidade de negócio.
public interface ClientRepository extends JpaRepository<Client, UUID> {

    Optional<Client> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<Client> findAllByOrganizationIdOrderByNameAsc(UUID organizationId);
}
