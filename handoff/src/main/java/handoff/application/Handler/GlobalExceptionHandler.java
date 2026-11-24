package handoff.application.Handler;

import handoff.application.Exception.AttachmentNotUploadedException;
import handoff.application.Exception.ClientHasProjectsException;
import handoff.application.Exception.InvalidAttachmentException;
import handoff.application.Exception.EmailAlreadyUsedException;
import handoff.application.Exception.ResourceNotFoundException;
import handoff.application.Exception.InvalidCredentialsException;
import handoff.application.Exception.InvalidRefreshTokenException;
import handoff.application.Exception.ForbiddenTransitionException;
import handoff.application.Exception.InvalidTransitionException;
import handoff.application.Exception.TooManyRequestsException;
import org.springframework.http.HttpHeaders;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(EmailAlreadyUsedException.class)
    public ProblemDetail handleEmailAlreadyUsed(EmailAlreadyUsedException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ClientHasProjectsException.class)
    public ProblemDetail handleClientHasProjects(ClientHasProjectsException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler({InvalidTransitionException.class, ObjectOptimisticLockingFailureException.class})
    public ProblemDetail handleConflict(RuntimeException e) {
        String detail = e instanceof ObjectOptimisticLockingFailureException
                ? "A solicitação foi alterada por outra pessoa; recarregue e tente de novo"
                : e.getMessage();
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, detail);
    }

    @ExceptionHandler(InvalidAttachmentException.class)
    public ProblemDetail handleInvalidAttachment(InvalidAttachmentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(AttachmentNotUploadedException.class)
    public ProblemDetail handleAttachmentNotUploaded(AttachmentNotUploadedException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(ForbiddenTransitionException.class)
    public ProblemDetail handleForbiddenTransition(ForbiddenTransitionException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler({InvalidCredentialsException.class, InvalidRefreshTokenException.class})
    public ProblemDetail handleUnauthorized(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ProblemDetail> handleTooManyRequests(TooManyRequestsException e) {
        long seconds = Math.max(1, (long) Math.ceil(e.getRetryAfter().toMillis() / 1000.0));
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(seconds))
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, e.getMessage()));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dados inválidos");
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }
}
