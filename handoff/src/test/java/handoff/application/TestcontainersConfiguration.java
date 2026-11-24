package handoff.application;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    public static final int MAILPIT_SMTP_PORT = 1025;
    public static final int MAILPIT_HTTP_PORT = 8025;

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:16"));
    }

    @Bean
    @ServiceConnection
    RabbitMQContainer rabbitContainer() {
        return new RabbitMQContainer(DockerImageName.parse("rabbitmq:4-management"));
    }

    @Bean
    MinIOContainer minioContainer() {
        return new MinIOContainer(DockerImageName.parse("cgr.dev/chainguard/minio:latest")
                .asCompatibleSubstituteFor("minio/minio"));
    }

    @Bean
    GenericContainer<?> mailpitContainer() {
        return new GenericContainer<>(DockerImageName.parse("axllent/mailpit:latest"))
                .withExposedPorts(MAILPIT_SMTP_PORT, MAILPIT_HTTP_PORT)
                .waitingFor(Wait.forHttp("/livez").forPort(MAILPIT_HTTP_PORT));
    }

    @Bean
    DynamicPropertyRegistrar externalServicesProperties(MinIOContainer minio, GenericContainer<?> mailpitContainer) {
        return registry -> {
            registry.add("storage.endpoint", minio::getS3URL);
            registry.add("storage.access-key", minio::getUserName);
            registry.add("storage.secret-key", minio::getPassword);
            registry.add("storage.bucket", () -> "handoff-test");
            registry.add("spring.mail.host", mailpitContainer::getHost);
            registry.add("spring.mail.port", () -> mailpitContainer.getMappedPort(MAILPIT_SMTP_PORT));
            registry.add("mailpit.api-url", () -> "http://%s:%d/api/v1".formatted(
                    mailpitContainer.getHost(), mailpitContainer.getMappedPort(MAILPIT_HTTP_PORT)));
        };
    }
}
