package handoff.application;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(properties = {
        "jwt.key=${random.uuid}${random.uuid}",
        "spring.datasource.password=test",
        "rate-limit.login-per-ip.requests=10000",
        "rate-limit.register-per-ip.requests=10000",
        "rate-limit.refresh-per-ip.requests=10000"
})
@Import(TestcontainersConfiguration.class)
class HandoffApplicationTests {

	@Test
	void contextLoads() {
	}

}
