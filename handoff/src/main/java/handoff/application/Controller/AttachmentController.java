package handoff.application.Controller;

import handoff.application.Dto.AttachmentResponseDto;
import handoff.application.Dto.DownloadUrlResponseDto;
import handoff.application.Dto.RequestUploadDto;
import handoff.application.Dto.UploadUrlResponseDto;
import handoff.application.Service.AttachmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/app/requests/{requestId}/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    @PostMapping("/upload-url")
    @ResponseStatus(HttpStatus.CREATED)
    public UploadUrlResponseDto requestUpload(@PathVariable UUID requestId, @RequestBody @Valid RequestUploadDto request) {
        return attachmentService.requestUpload(requestId, request);
    }

    @PostMapping("/{attachmentId}/complete")
    public AttachmentResponseDto complete(@PathVariable UUID requestId, @PathVariable UUID attachmentId) {
        return attachmentService.complete(requestId, attachmentId);
    }

    @GetMapping
    public List<AttachmentResponseDto> list(@PathVariable UUID requestId) {
        return attachmentService.list(requestId);
    }

    @GetMapping("/{attachmentId}/download-url")
    public DownloadUrlResponseDto downloadUrl(@PathVariable UUID requestId, @PathVariable UUID attachmentId) {
        return attachmentService.downloadUrl(requestId, attachmentId);
    }
}
