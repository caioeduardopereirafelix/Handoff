package handoff.application.Config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "storage")
public record StorageProperties(
        String endpoint,
        String publicEndpoint,
        @NotBlank String region,
        @NotBlank String bucket,
        String accessKey,
        String secretKey,
        String fallbackAccessKey,
        String fallbackSecretKey,
        @Positive long maxSizeBytes,
        @NotNull Duration urlExpiration
) {

    public String effectivePublicEndpoint() {
        return StringUtils.hasText(publicEndpoint) ? publicEndpoint : endpoint;
    }

    public String effectiveAccessKey() {
        return StringUtils.hasText(accessKey) ? accessKey : fallbackAccessKey;
    }

    public String effectiveSecretKey() {
        return StringUtils.hasText(secretKey) ? secretKey : fallbackSecretKey;
    }

    @AssertTrue(message = "defina STORAGE_ACCESS_KEY e STORAGE_SECRET_KEY, ou MINIO_ROOT_USER e MINIO_ROOT_PASSWORD")
    public boolean isCredentialsPresent() {
        return StringUtils.hasText(effectiveAccessKey()) && StringUtils.hasText(effectiveSecretKey());
    }
}
