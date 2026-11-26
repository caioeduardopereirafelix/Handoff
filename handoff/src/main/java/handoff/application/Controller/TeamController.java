package handoff.application.Controller;

import handoff.application.Dto.TeamMemberDto;
import handoff.application.Dto.TeamResponseDto;
import handoff.application.Dto.UpdateMemberDto;
import handoff.application.Entity.AppUser;
import handoff.application.Service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/app/team")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @GetMapping
    public TeamResponseDto list() {
        return teamService.list();
    }

    @PreAuthorize("hasRole('OWNER')")
    @PatchMapping("/members/{id}")
    public TeamMemberDto update(@PathVariable UUID id, @RequestBody UpdateMemberDto request,
                                @AuthenticationPrincipal AppUser user) {
        return teamService.update(id, request, user);
    }
}
