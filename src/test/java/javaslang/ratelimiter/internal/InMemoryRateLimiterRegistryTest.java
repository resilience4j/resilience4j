package javaslang.ratelimiter.internal;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javaslang.ratelimiter.RateLimiter;
import javaslang.ratelimiter.RateLimiterConfig;
import javaslang.ratelimiter.RateLimiterRegistry;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.time.Duration;
import java.util.function.Supplier;


public class InMemoryRateLimiterRegistryTest {

    private static final int LIMIT = 50;
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REFRESH_PERIOD = Duration.ofNanos(500);
    private static final String CONFIG_MUST_NOT_BE_NULL = "Config must not be null";
    private static final String NAME_MUST_NOT_BE_NULL = "Name must not be null";
    @Rule
    public ExpectedException exception = ExpectedException.none();
    private RateLimiterConfig config;

    @Before
    public void init() {
        config = RateLimiterConfig.builder()
            .timeoutDuration(TIMEOUT)
            .limitRefreshPeriod(REFRESH_PERIOD)
            .limitForPeriod(LIMIT)
            .build();
    }

    @Test
    public void rateLimiterPositive() throws Exception {
        RateLimiterRegistry registry = RateLimiterRegistry.of(config);
        RateLimiter firstRateLimiter = registry.rateLimiter("test");
        RateLimiter anotherLimit = registry.rateLimiter("test1");
        RateLimiter sameAsFirst = registry.rateLimiter("test");

        then(firstRateLimiter).isEqualTo(sameAsFirst);
        then(firstRateLimiter).isNotEqualTo(anotherLimit);
    }

    @Test
    public void rateLimiterPositiveWithSupplier() throws Exception {
        RateLimiterRegistry registry = new InMemoryRateLimiterRegistry(config);
        Supplier<RateLimiterConfig> rateLimiterConfigSupplier = mock(Supplier.class);
        when(rateLimiterConfigSupplier.get())
            .thenReturn(config);

        RateLimiter firstRateLimiter = registry.rateLimiter("test", rateLimiterConfigSupplier);
        verify(rateLimiterConfigSupplier, times(1)).get();
        RateLimiter sameAsFirst = registry.rateLimiter("test", rateLimiterConfigSupplier);
        verify(rateLimiterConfigSupplier, times(1)).get();
        RateLimiter anotherLimit = registry.rateLimiter("test1", rateLimiterConfigSupplier);
        verify(rateLimiterConfigSupplier, times(2)).get();

        then(firstRateLimiter).isEqualTo(sameAsFirst);
        then(firstRateLimiter).isNotEqualTo(anotherLimit);
    }

    @Test
    public void rateLimiterConfigIsNull() throws Exception {
        exception.expect(NullPointerException.class);
        exception.expectMessage(CONFIG_MUST_NOT_BE_NULL);
        new InMemoryRateLimiterRegistry(null);
    }

    @Test
    public void rateLimiterNewWithNullName() throws Exception {
        exception.expect(NullPointerException.class);
        exception.expectMessage(NAME_MUST_NOT_BE_NULL);
        RateLimiterRegistry registry = new InMemoryRateLimiterRegistry(config);
        registry.rateLimiter(null);
    }

    @Test
    public void rateLimiterNewWithNullNonDefaultConfig() throws Exception {
        exception.expect(NullPointerException.class);
        exception.expectMessage(CONFIG_MUST_NOT_BE_NULL);
        RateLimiterRegistry registry = new InMemoryRateLimiterRegistry(config);
        RateLimiterConfig rateLimiterConfig = null;
        registry.rateLimiter("name", rateLimiterConfig);
    }

    @Test
    public void rateLimiterNewWithNullNameAndNonDefaultConfig() throws Exception {
        exception.expect(NullPointerException.class);
        exception.expectMessage(NAME_MUST_NOT_BE_NULL);
        RateLimiterRegistry registry = new InMemoryRateLimiterRegistry(config);
        registry.rateLimiter(null, config);
    }

    @Test
    public void rateLimiterNewWithNullNameAndConfigSupplier() throws Exception {
        exception.expect(NullPointerException.class);
        exception.expectMessage(NAME_MUST_NOT_BE_NULL);
        RateLimiterRegistry registry = new InMemoryRateLimiterRegistry(config);
        registry.rateLimiter(null, () -> config);
    }

    @Test
    public void rateLimiterNewWithNullConfigSupplier() throws Exception {
        exception.expect(NullPointerException.class);
        exception.expectMessage("Supplier must not be null");
        RateLimiterRegistry registry = new InMemoryRateLimiterRegistry(config);
        Supplier<RateLimiterConfig> rateLimiterConfigSupplier = null;
        registry.rateLimiter("name", rateLimiterConfigSupplier);
    }
}