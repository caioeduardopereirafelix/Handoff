package handoff.application.Controller;

import handoff.application.Config.PortalPrincipal;
import handoff.application.Dto.ActivityLogResponseDto;
import handoff.application.Dto.AttachmentResponseDto;
import handoff.application.Dto.DownloadUrlResponseDto;
import handoff.application.Dto.RequestUploadDto;
import handoff.application.Dto.UploadUrlResponseDto;
import handoff.application.Dto.CommentResponseDto;
import handoff.application.Dto.CreateCommentDto;
import handoff.application.Dto.CreateRequestDto;
import handoff.application.Dto.PageResponseDto;
import handoff.application.Dto.PortalProjectDto;
import handoff.application.Dto.RequestResponseDto;
import handoff.application.Entity.ActorType;
import handoff.application.Entity.RequestStatus;
import handoff.application.Service.ActivityLogService;
import handoff.application.Service.AttachmentService;
import handoff.application.Service.CommentService;
import handoff.application.Service.RequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/portal/{token}")
@RequiredArgsConstructor
public class PortalController {

    private final RequestService requestService;
    private final CommentService commentService;
    private final ActivityLogService activityLogService;
    private final AttachmentService attachmentService;

    @GetMapping("/project")
    public PortalProjectDto project(@AuthenticationPrincipal PortalPrincipal portal) {
        return requestService.portalProject(portal.projectId());
    }

    @GetMapping("/requests")
    public PageResponseDto<RequestResponseDto> list(@AuthenticationPrincipal PortalPrincipal portal,
                                                    @RequestParam(required = false) Set<RequestStatus> status,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "50") int size) {
        return requestService.listForProject(portal.projectId(), status, page, size);
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public RequestResponseDto create(@AuthenticationPrincipal PortalPrincipal portal,
                                     @RequestBody @Valid CreateRequestDto request) {
        return requestService.create(portal.projectId(), request, ActorType.CLIENT);
    }

    @GetMapping("/requests/{id}")
    public RequestResponseDto get(@AuthenticationPrincipal PortalPrincipal portal, @PathVariable UUID id) {
        return requestService.getInProject(portal.projectId(), id);
    }

    @GetMapping("/requests/{id}/activity")
    public List<ActivityLogResponseDto> activity(@AuthenticationPrincipal PortalPrincipal portal, @PathVariable UUID id) {
        return activityLogService.listInProject(portal.projectId(), id);
    }

    @PostMapping("/requests/{id}/attachments/upload-url")
    @ResponseStatus(HttpStatus.CREATED)
    public UploadUrlResponseDto requestUpload(@AuthenticationPrincipal PortalPrincipal portal,
                                              @PathVariable UUID id,
                                              @RequestBody @Valid RequestUploadDto request) {
        return attachmentService.requestUploadInProject(portal.projectId(), id, request);
    }

    @PostMapping("/requests/{id}/attachments/{attachmentId}/complete")
    public AttachmentResponseDto completeUpload(@AuthenticationPrincipal PortalPrincipal portal,
                                                @PathVariable UUID id,
                                                @PathVariable UUID attachmentId) {
        return attachmentService.completeInProject(portal.projectId(), id, attachmentId);
    }

    @GetMapping("/requests/{id}/attachments")
    public List<AttachmentResponseDto> attachments(@AuthenticationPrincipal PortalPrincipal portal, @PathVariable UUID id) {
        return attachmentService.listInProject(portal.projectId(), id);
    }

    @GetMapping("/requests/{id}/attachments/{attachmentId}/download-url")
    public DownloadUrlResponseDto downloadUrl(@AuthenticationPrincipal PortalPrincipal portal,
                                              @PathVariable UUID id,
                                              @PathVariable UUID attachmentId) {
        return attachmentService.downloadUrlInProject(portal.projectId(), id, attachmentId);
    }

    @GetMapping("/requests/{id}/comments")
    public List<CommentResponseDto> comments(@AuthenticationPrincipal PortalPrincipal portal, @PathVariable UUID id) {
        return commentService.listInProject(portal.projectId(), id);
    }

    @PostMapping("/requests/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponseDto comment(@AuthenticationPrincipal PortalPrincipal portal,
                                      @PathVariable UUID id,
                                      @RequestBody @Valid CreateCommentDto request) {
        return commentService.addByClient(id, request, portal);
    }

    @PostMapping("/requests/{id}/approve")
    public RequestResponseDto approve(@AuthenticationPrincipal PortalPrincipal portal, @PathVariable UUID id) {
        return requestService.changeStatusInProject(portal.projectId(), id, RequestStatus.DONE);
    }

    @PostMapping("/requests/{id}/request-changes")
    public RequestResponseDto requestChanges(@AuthenticationPrincipal PortalPrincipal portal, @PathVariable UUID id) {
        return requestService.changeStatusInProject(portal.projectId(), id, RequestStatus.CHANGES_REQUESTED);
    }
}
