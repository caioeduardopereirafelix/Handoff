package handoff.application.Config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "notifications")
public record NotificationProperties(
        boolean enabled,
        @NotBlank String from,
        @NotBlank String portalBaseUrl
) {
}
