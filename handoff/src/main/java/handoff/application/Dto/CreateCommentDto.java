package handoff.application.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentDto(
        @NotBlank @Size(max = 5000) String body
) {
}
