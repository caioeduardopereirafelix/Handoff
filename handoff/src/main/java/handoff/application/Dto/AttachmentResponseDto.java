package handoff.application.Dto;

import handoff.application.Entity.ActorType;
import handoff.application.Entity.Attachment;

import java.time.Instant;
import java.util.UUID;

public record AttachmentResponseDto(
        UUID id,
        UUID requestId,
        UUID commentId,
        String originalName,
        String contentType,
        long sizeBytes,
        ActorType uploadedByType,
        String uploadedByName,
        Instant createdAt,
        Instant completedAt
) {

    public static AttachmentResponseDto from(Attachment attachment, String clientName) {
        String uploadedBy = attachment.getUploadedByType() == ActorType.CLIENT
                ? clientName
                : attachment.getUploadedByUser() == null ? "Usuário removido" : attachment.getUploadedByUser().getName();
        return new AttachmentResponseDto(
                attachment.getId(),
                attachment.getRequest().getId(),
                attachment.getComment() == null ? null : attachment.getComment().getId(),
                attachment.getOriginalName(),
                attachment.getContentType(),
                attachment.getSizeBytes(),
                attachment.getUploadedByType(),
                uploadedBy,
                attachment.getCreatedAt(),
                attachment.getCompletedAt());
    }
}
