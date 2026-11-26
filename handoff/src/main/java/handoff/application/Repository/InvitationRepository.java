package handoff.application.Repository;

import handoff.application.Entity.Invitation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    @EntityGraph(attributePaths = "invitedBy")
    Optional<Invitation> findByTokenHash(String tokenHash);

    Optional<Invitation> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<Invitation> findAllByOrganizationIdAndEmailAndAcceptedAtIsNullAndRevokedAtIsNull(UUID organizationId, String email);
}
