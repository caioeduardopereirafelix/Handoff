package handoff.application.Controller;

import handoff.application.Dto.CommentResponseDto;
import handoff.application.Dto.CreateCommentDto;
import handoff.application.Entity.AppUser;
import handoff.application.Service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/app/requests/{requestId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    public List<CommentResponseDto> list(@PathVariable UUID requestId) {
        return commentService.list(requestId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponseDto add(@PathVariable UUID requestId,
                                  @RequestBody @Valid CreateCommentDto request,
                                  @AuthenticationPrincipal AppUser user) {
        return commentService.addByUser(requestId, request, user);
    }
}
