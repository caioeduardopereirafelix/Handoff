package handoff.application.Repository;

import handoff.application.Entity.AppUser;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

// O login busca o usuário por e-mail antes de existir um tenant autenticado,
// por isso este repositório não filtra por organization_id.
public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    @EntityGraph(attributePaths = "organization")
    Optional<AppUser> findByEmail(String email);

    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = "organization")
    Optional<AppUser> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
