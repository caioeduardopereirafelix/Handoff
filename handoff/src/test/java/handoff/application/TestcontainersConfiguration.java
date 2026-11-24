package handoff.application;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:16"));
    }

    @Bean
    MinIOContainer minioContainer() {
        return new MinIOContainer(DockerImageName.parse("cgr.dev/chainguard/minio:latest")
                .asCompatibleSubstituteFor("minio/minio"));
    }

    @Bean
    DynamicPropertyRegistrar storageProperties(MinIOContainer minio) {
        return registry -> {
            registry.add("storage.endpoint", minio::getS3URL);
            registry.add("storage.access-key", minio::getUserName);
            registry.add("storage.secret-key", minio::getPassword);
            registry.add("storage.bucket", () -> "handoff-test");
        };
    }
}
