package handoff.application.Config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "rate-limit")
public record RateLimitProperties(
        boolean enabled,
        @Valid @NotNull Limit loginPerIp,
        @Valid @NotNull Limit loginPerEmail,
        @Valid @NotNull Limit registerPerIp,
        @Valid @NotNull Limit refreshPerIp
) {

    public record Limit(@Positive int requests, @NotNull Duration period) {
    }
}
