package handoff.application.Controller;

import handoff.application.Dto.AssignRequestDto;
import handoff.application.Dto.ChangeRequestStatusDto;
import handoff.application.Dto.CreateRequestDto;
import handoff.application.Dto.RequestResponseDto;
import handoff.application.Entity.ActorType;
import handoff.application.Service.RequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/app")
@RequiredArgsConstructor
public class RequestController {

    private final RequestService requestService;

    @GetMapping("/projects/{projectId}/requests")
    public List<RequestResponseDto> list(@PathVariable UUID projectId) {
        return requestService.listForProject(projectId);
    }

    @PostMapping("/projects/{projectId}/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public RequestResponseDto create(@PathVariable UUID projectId, @RequestBody @Valid CreateRequestDto request) {
        return requestService.create(projectId, request, ActorType.USER);
    }

    @GetMapping("/requests/{id}")
    public RequestResponseDto get(@PathVariable UUID id) {
        return requestService.get(id);
    }

    @PatchMapping("/requests/{id}/status")
    public RequestResponseDto changeStatus(@PathVariable UUID id, @RequestBody @Valid ChangeRequestStatusDto request) {
        return requestService.changeStatus(id, request.status());
    }

    @PutMapping("/requests/{id}/assignment")
    public RequestResponseDto assign(@PathVariable UUID id, @RequestBody AssignRequestDto request) {
        return requestService.assign(id, request);
    }
}
