package handoff.application.Service;

import handoff.application.Config.RateLimitProperties;
import handoff.application.Config.RateLimitProperties.Limit;
import handoff.application.Exception.TooManyRequestsException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimiterTest {

    private static RateLimiter limiter(boolean enabled) {
        Limit two = new Limit(2, Duration.ofMinutes(1));
        return new RateLimiter(new RateLimitProperties(enabled, two, two, two, two));
    }

    @Test
    void blocksAfterTheLimitAndSaysHowLongToWait() {
        RateLimiter limiter = limiter(true);
        limiter.checkRegister("10.0.0.1");
        limiter.checkRegister("10.0.0.1");

        assertThatThrownBy(() -> limiter.checkRegister("10.0.0.1"))
                .isInstanceOfSatisfying(TooManyRequestsException.class, e -> {
                    assertThat(e.getRetryAfter()).isPositive().isLessThanOrEqualTo(Duration.ofMinutes(1));
                    assertThat(e.getMessage()).startsWith("Muitas tentativas");
                });
    }

    @Test
    void eachIpHasItsOwnLimit() {
        RateLimiter limiter = limiter(true);
        limiter.checkRefresh("10.0.0.1");
        limiter.checkRefresh("10.0.0.1");

        assertThatCode(() -> limiter.checkRefresh("10.0.0.2")).doesNotThrowAnyException();
    }

    @Test
    void loginLimitPerEmailIgnoresCaseAndSpacesAcrossIps() {
        RateLimiter limiter = limiter(true);
        limiter.checkLogin("10.0.0.1", "Ana@Example.com");
        limiter.checkLogin("10.0.0.2", " ana@example.com ");

        assertThatThrownBy(() -> limiter.checkLogin("10.0.0.3", "ANA@EXAMPLE.COM"))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void endpointsDoNotShareLimits() {
        RateLimiter limiter = limiter(true);
        limiter.checkRegister("10.0.0.1");
        limiter.checkRegister("10.0.0.1");

        assertThatCode(() -> limiter.checkLogin("10.0.0.1", "ana@example.com")).doesNotThrowAnyException();
    }

    @Test
    void doesNothingWhenDisabled() {
        RateLimiter limiter = limiter(false);

        assertThatCode(() -> {
            for (int i = 0; i < 10; i++) {
                limiter.checkRegister("10.0.0.1");
            }
        }).doesNotThrowAnyException();
    }
}
