package handoff.application.Service;

import handoff.application.Config.PortalPrincipal;
import handoff.application.Config.TenantContext;
import handoff.application.Dto.CommentResponseDto;
import handoff.application.Dto.CreateCommentDto;
import handoff.application.Entity.ActorType;
import handoff.application.Entity.AppUser;
import handoff.application.Entity.Comment;
import handoff.application.Entity.Request;
import handoff.application.Exception.ResourceNotFoundException;
import handoff.application.Repository.CommentRepository;
import handoff.application.Repository.RequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final RequestRepository requestRepository;
    private final TenantContext tenantContext;

    @Transactional(readOnly = true)
    public List<CommentResponseDto> list(UUID requestId) {
        return list(findRequest(requestId));
    }

    @Transactional(readOnly = true)
    public List<CommentResponseDto> listInProject(UUID projectId, UUID requestId) {
        return list(findRequestInProject(projectId, requestId));
    }

    @Transactional
    public CommentResponseDto addByUser(UUID requestId, CreateCommentDto dto, AppUser author) {
        Comment comment = newComment(findRequest(requestId), dto, ActorType.USER);
        comment.setAuthorUser(author);
        return save(comment);
    }

    @Transactional
    public CommentResponseDto addByClient(UUID requestId, CreateCommentDto dto, PortalPrincipal portal) {
        Comment comment = newComment(findRequestInProject(portal.projectId(), requestId), dto, ActorType.CLIENT);
        comment.setPortalAccessId(portal.accessId());
        return save(comment);
    }

    private List<CommentResponseDto> list(Request request) {
        String clientName = clientNameOf(request);
        return commentRepository
                .findAllByOrganizationIdAndRequestIdOrderByCreatedAtAsc(tenantContext.getOrganizationId(), request.getId())
                .stream()
                .map(comment -> CommentResponseDto.from(comment, clientName))
                .toList();
    }

    private Comment newComment(Request request, CreateCommentDto dto, ActorType authorType) {
        Comment comment = new Comment();
        comment.setRequest(request);
        comment.setAuthorType(authorType);
        comment.setBody(dto.body().trim());
        return comment;
    }

    private CommentResponseDto save(Comment comment) {
        Comment saved = commentRepository.saveAndFlush(comment);
        return CommentResponseDto.from(saved, clientNameOf(saved.getRequest()));
    }

    private String clientNameOf(Request request) {
        return request.getProject().getClient().getName();
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
