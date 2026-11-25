package handoff.application.Config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class StoragePropertiesTest {

    @Test
    void emptyExplicitCredentialsFallBackToMinio() {
        StorageProperties properties = properties("", "", "handoff", "handoff123");

        assertThat(properties.effectiveAccessKey()).isEqualTo("handoff");
        assertThat(properties.effectiveSecretKey()).isEqualTo("handoff123");
        assertThat(properties.isCredentialsPresent()).isTrue();
    }

    @Test
    void explicitCredentialsWinOverMinio() {
        StorageProperties properties = properties("aws-key", "aws-secret", "handoff", "handoff123");

        assertThat(properties.effectiveAccessKey()).isEqualTo("aws-key");
        assertThat(properties.effectiveSecretKey()).isEqualTo("aws-secret");
    }

    @Test
    void missingEverywhereIsInvalid() {
        assertThat(properties("", null, "", null).isCredentialsPresent()).isFalse();
    }

    @Test
    void presignedUrlsUseThePublicEndpointWhenSet() {
        assertThat(withPublicEndpoint("https://handoff.example.com").effectivePublicEndpoint())
                .isEqualTo("https://handoff.example.com");
        assertThat(withPublicEndpoint("").effectivePublicEndpoint()).isEqualTo("http://minio:9000");
    }

    private static StorageProperties properties(String accessKey, String secretKey,
                                                String fallbackAccessKey, String fallbackSecretKey) {
        return new StorageProperties("http://localhost:9000", null, "us-east-1", "handoff",
                accessKey, secretKey, fallbackAccessKey, fallbackSecretKey, 1024, Duration.ofMinutes(5));
    }

    private static StorageProperties withPublicEndpoint(String publicEndpoint) {
        return new StorageProperties("http://minio:9000", publicEndpoint, "us-east-1", "handoff",
                "key", "secret", null, null, 1024, Duration.ofMinutes(5));
    }
}
