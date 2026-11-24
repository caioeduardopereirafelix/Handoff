package handoff.application.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import handoff.application.Config.RateLimitProperties;
import handoff.application.Config.RateLimitProperties.Limit;
import handoff.application.Exception.TooManyRequestsException;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;

@Service
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimiter {

    private final RateLimitProperties properties;
    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofHours(2))
            .maximumSize(100_000)
            .build();

    public RateLimiter(RateLimitProperties properties) {
        this.properties = properties;
    }

    public void checkLogin(String ip, String email) {
        consume("login-ip:" + ip, properties.loginPerIp());
        consume("login-email:" + normalize(email), properties.loginPerEmail());
    }

    public void checkRegister(String ip) {
        consume("register-ip:" + ip, properties.registerPerIp());
    }

    public void checkRefresh(String ip) {
        consume("refresh-ip:" + ip, properties.refreshPerIp());
    }

    private void consume(String key, Limit limit) {
        if (!properties.enabled()) {
            return;
        }
        Bucket bucket = buckets.get(key, ignored -> newBucket(limit));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            throw new TooManyRequestsException(Duration.ofNanos(probe.getNanosToWaitForRefill()));
        }
    }

    private static Bucket newBucket(Limit limit) {
        return Bucket.builder()
                .addLimit(bandwidth -> bandwidth
                        .capacity(limit.requests())
                        .refillGreedy(limit.requests(), limit.period()))
                .build();
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
