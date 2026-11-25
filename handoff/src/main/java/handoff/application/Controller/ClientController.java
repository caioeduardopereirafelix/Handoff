package handoff.application.Controller;

import handoff.application.Dto.ClientRequestDto;
import handoff.application.Dto.ClientResponseDto;
import handoff.application.Dto.PageResponseDto;
import handoff.application.Service.ClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/app/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @GetMapping
    public PageResponseDto<ClientResponseDto> list(@RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "50") int size) {
        return clientService.list(page, size);
    }

    @GetMapping("/{id}")
    public ClientResponseDto get(@PathVariable UUID id) {
        return clientService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClientResponseDto create(@RequestBody @Valid ClientRequestDto request) {
        return clientService.create(request);
    }

    @PutMapping("/{id}")
    public ClientResponseDto update(@PathVariable UUID id, @RequestBody @Valid ClientRequestDto request) {
        return clientService.update(id, request);
    }

    @PreAuthorize("hasRole('OWNER')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        clientService.delete(id);
    }
}
