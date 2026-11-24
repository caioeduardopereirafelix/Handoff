package handoff.application.Dto;

import handoff.application.Entity.ActorType;
import handoff.application.Entity.Comment;

import java.time.Instant;
import java.util.UUID;

public record CommentResponseDto(
        UUID id,
        UUID requestId,
        ActorType authorType,
        String authorName,
        String body,
        Instant createdAt
) {

    public static CommentResponseDto from(Comment comment, String clientName) {
        String authorName = comment.getAuthorType() == ActorType.CLIENT
                ? clientName
                : comment.getAuthorUser() == null ? "Usuário removido" : comment.getAuthorUser().getName();
        return new CommentResponseDto(comment.getId(), comment.getRequest().getId(), comment.getAuthorType(),
                authorName, comment.getBody(), comment.getCreatedAt());
    }
}
