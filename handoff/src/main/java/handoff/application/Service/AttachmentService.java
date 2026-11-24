package handoff.application.Service;

import handoff.application.Config.PortalPrincipal;
import handoff.application.Config.StorageProperties;
import handoff.application.Config.TenantContext;
import handoff.application.Dto.AttachmentResponseDto;
import handoff.application.Dto.DownloadUrlResponseDto;
import handoff.application.Dto.RequestUploadDto;
import handoff.application.Dto.UploadUrlResponseDto;
import handoff.application.Entity.ActivityAction;
import handoff.application.Entity.ActorType;
import handoff.application.Entity.AppUser;
import handoff.application.Entity.Attachment;
import handoff.application.Entity.AttachmentStatus;
import handoff.application.Entity.Request;
import handoff.application.Exception.AttachmentNotUploadedException;
import handoff.application.Exception.InvalidAttachmentException;
import handoff.application.Exception.ResourceNotFoundException;
import handoff.application.Repository.AttachmentRepository;
import handoff.application.Repository.CommentRepository;
import handoff.application.Repository.RequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttachmentService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/png", "image/jpeg", "image/webp", "image/gif",
            "application/pdf", "text/plain", "text/csv", "application/zip",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation");

    private final AttachmentRepository attachmentRepository;
    private final RequestRepository requestRepository;
    private final CommentRepository commentRepository;
    private final ActivityLogService activityLogService;
    private final TenantContext tenantContext;
    private final StorageProperties storageProperties;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final Clock clock;

    @Transactional
    public UploadUrlResponseDto requestUpload(UUID requestId, RequestUploadDto dto) {
        return requestUpload(findRequest(requestId), dto);
    }

    @Transactional
    public UploadUrlResponseDto requestUploadInProject(UUID projectId, UUID requestId, RequestUploadDto dto) {
        return requestUpload(findRequestInProject(projectId, requestId), dto);
    }

    @Transactional
    public AttachmentResponseDto complete(UUID requestId, UUID attachmentId) {
        return complete(findRequest(requestId), attachmentId);
    }

    @Transactional
    public AttachmentResponseDto completeInProject(UUID projectId, UUID requestId, UUID attachmentId) {
        return complete(findRequestInProject(projectId, requestId), attachmentId);
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponseDto> list(UUID requestId) {
        return list(findRequest(requestId));
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponseDto> listInProject(UUID projectId, UUID requestId) {
        return list(findRequestInProject(projectId, requestId));
    }

    @Transactional(readOnly = true)
    public DownloadUrlResponseDto downloadUrl(UUID requestId, UUID attachmentId) {
        return downloadUrl(findRequest(requestId), attachmentId);
    }

    @Transactional(readOnly = true)
    public DownloadUrlResponseDto downloadUrlInProject(UUID projectId, UUID requestId, UUID attachmentId) {
        return downloadUrl(findRequestInProject(projectId, requestId), attachmentId);
    }

    private UploadUrlResponseDto requestUpload(Request request, RequestUploadDto dto) {
        String contentType = dto.contentType().trim().toLowerCase();
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new InvalidAttachmentException("Tipo de arquivo não permitido: " + contentType);
        }
        if (dto.sizeBytes() > storageProperties.maxSizeBytes()) {
            throw new InvalidAttachmentException("Arquivo maior que o limite de "
                    + storageProperties.maxSizeBytes() / (1024 * 1024) + " MB");
        }

        Attachment attachment = new Attachment();
        attachment.setRequest(request);
        if (dto.commentId() != null) {
            attachment.setComment(commentRepository
                    .findByIdAndOrganizationIdAndRequestId(dto.commentId(), tenantContext.getOrganizationId(), request.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Comentário")));
        }
        attachment.setStorageKey("org/%s/request/%s/%s".formatted(
                tenantContext.getOrganizationId(), request.getId(), UUID.randomUUID()));
        attachment.setOriginalName(cleanFileName(dto.fileName()));
        attachment.setContentType(contentType);
        attachment.setSizeBytes(dto.sizeBytes());
        setUploader(attachment);
        attachmentRepository.saveAndFlush(attachment);

        PutObjectRequest putObject = PutObjectRequest.builder()
                .bucket(storageProperties.bucket())
                .key(attachment.getStorageKey())
                .contentType(contentType)
                .contentLength(dto.sizeBytes())
                .build();
        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(PutObjectPresignRequest.builder()
                .signatureDuration(storageProperties.urlExpiration())
                .putObjectRequest(putObject)
                .build());

        Map<String, String> headers = new LinkedHashMap<>();
        presigned.signedHeaders().forEach((name, values) -> {
            if (!name.equalsIgnoreCase("host")) {
                headers.put(name, String.join(",", values));
            }
        });
        return new UploadUrlResponseDto(attachment.getId(), presigned.url().toString(), "PUT", headers,
                presigned.expiration());
    }

    private AttachmentResponseDto complete(Request request, UUID attachmentId) {
        Attachment attachment = findAttachment(request, attachmentId);
        if (attachment.getStatus() == AttachmentStatus.READY) {
            return AttachmentResponseDto.from(attachment, clientNameOf(request));
        }

        HeadObjectResponse head = headObject(attachment.getStorageKey())
                .orElseThrow(AttachmentNotUploadedException::new);
        if (head.contentLength() == null || head.contentLength() != attachment.getSizeBytes()) {
            throw new AttachmentNotUploadedException();
        }
        if (!FileSignature.matches(attachment.getContentType(), readHead(attachment.getStorageKey()))) {
            deleteObject(attachment.getStorageKey());
            throw new InvalidAttachmentException("O conteúdo do arquivo não corresponde ao tipo informado");
        }

        attachment.setStatus(AttachmentStatus.READY);
        attachment.setCompletedAt(clock.instant());
        attachmentRepository.saveAndFlush(attachment);
        activityLogService.record(request, ActivityAction.ATTACHMENT_ADDED);
        return AttachmentResponseDto.from(attachment, clientNameOf(request));
    }

    private List<AttachmentResponseDto> list(Request request) {
        String clientName = clientNameOf(request);
        return attachmentRepository
                .findAllByOrganizationIdAndRequestIdAndStatusOrderByCreatedAtAsc(
                        tenantContext.getOrganizationId(), request.getId(), AttachmentStatus.READY)
                .stream()
                .map(attachment -> AttachmentResponseDto.from(attachment, clientName))
                .toList();
    }

    private DownloadUrlResponseDto downloadUrl(Request request, UUID attachmentId) {
        Attachment attachment = findAttachment(request, attachmentId);
        if (attachment.getStatus() != AttachmentStatus.READY) {
            throw new ResourceNotFoundException("Anexo");
        }
        String encodedName = URLEncoder.encode(attachment.getOriginalName(), StandardCharsets.UTF_8).replace("+", "%20");
        GetObjectRequest getObject = GetObjectRequest.builder()
                .bucket(storageProperties.bucket())
                .key(attachment.getStorageKey())
                .responseContentType(attachment.getContentType())
                .responseContentDisposition("attachment; filename*=UTF-8''" + encodedName)
                .build();
        var presigned = s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
                .signatureDuration(storageProperties.urlExpiration())
                .getObjectRequest(getObject)
                .build());
        return new DownloadUrlResponseDto(presigned.url().toString(), presigned.expiration());
    }

    private byte[] readHead(String key) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(storageProperties.bucket())
                .key(key)
                .range("bytes=0-" + (FileSignature.BYTES_TO_READ - 1))
                .build();
        try (ResponseInputStream<GetObjectResponse> stream = s3Client.getObject(request)) {
            return stream.readNBytes(FileSignature.BYTES_TO_READ);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void deleteObject(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(storageProperties.bucket())
                .key(key)
                .build());
    }

    private Optional<HeadObjectResponse> headObject(String key) {
        try {
            return Optional.of(s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(storageProperties.bucket())
                    .key(key)
                    .build()));
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }

    private void setUploader(Attachment attachment) {
        Optional<PortalPrincipal> portal = tenantContext.findPortalPrincipal();
        if (portal.isPresent()) {
            attachment.setUploadedByType(ActorType.CLIENT);
            attachment.setPortalAccessId(portal.get().accessId());
            return;
        }
        AppUser user = tenantContext.findCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Envio sem autor autenticado"));
        attachment.setUploadedByType(ActorType.USER);
        attachment.setUploadedByUser(user);
    }

    private static String cleanFileName(String fileName) {
        String name = fileName.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "").trim();
        if (name.isEmpty()) {
            throw new InvalidAttachmentException("Nome de arquivo inválido");
        }
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }

    private String clientNameOf(Request request) {
        return request.getProject().getClient().getName();
    }

    private Attachment findAttachment(Request request, UUID attachmentId) {
        return attachmentRepository
                .findByIdAndOrganizationIdAndRequestId(attachmentId, tenantContext.getOrganizationId(), request.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Anexo"));
    }

    private Request findRequest(UUID requestId) {
        return requestRepository.findByIdAndOrganizationId(requestId, tenantContext.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação"));
    }

    private Request findRequestInProject(UUID projectId, UUID requestId) {
        return requestRepository.findByIdAndOrganizationIdAndProjectId(requestId, tenantContext.getOrganizationId(), projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação"));
    }
}
