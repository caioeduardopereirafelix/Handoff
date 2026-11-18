package handoff.application;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(properties = {
        "handoff.jwt.secret=${random.uuid}${random.uuid}",
        "spring.datasource.password=test"
})
@Import(TestcontainersConfiguration.class)
class HandoffApplicationTests {

	@Test
	void contextLoads() {
	}

}
