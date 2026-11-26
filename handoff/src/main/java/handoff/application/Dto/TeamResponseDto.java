package handoff.application.Dto;

import java.util.List;

public record TeamResponseDto(
        List<TeamMemberDto> members,
        List<PendingInvitationDto> invitations
) {
}
