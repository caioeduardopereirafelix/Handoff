package handoff.application.Repository;

import handoff.application.Entity.Comment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    @EntityGraph(attributePaths = "authorUser")
    List<Comment> findAllByOrganizationIdAndRequestIdOrderByCreatedAtAsc(UUID organizationId, UUID requestId);
}
