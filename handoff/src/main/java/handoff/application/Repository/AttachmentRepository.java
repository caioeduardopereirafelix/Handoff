package handoff.application.Repository;

import handoff.application.Entity.Attachment;
import handoff.application.Entity.AttachmentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {

    @Query("select a.storageKey from Attachment a where a.organizationId = :organizationId and a.request.project.id = :projectId")
    List<String> findStorageKeysOfProject(@Param("organizationId") UUID organizationId, @Param("projectId") UUID projectId);

    @EntityGraph(attributePaths = {"uploadedByUser", "comment"})
    Optional<Attachment> findByIdAndOrganizationIdAndRequestId(UUID id, UUID organizationId, UUID requestId);

    @EntityGraph(attributePaths = {"uploadedByUser", "comment"})
    List<Attachment> findAllByOrganizationIdAndRequestIdAndStatusOrderByCreatedAtAsc(
            UUID organizationId, UUID requestId, AttachmentStatus status);
}
