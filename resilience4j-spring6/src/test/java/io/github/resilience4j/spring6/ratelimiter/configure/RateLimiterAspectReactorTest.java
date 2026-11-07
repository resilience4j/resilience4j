package io.github.resilience4j.spring6.ratelimiter.configure;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.event.RateLimiterEvent;
import io.github.resilience4j.spring6.fallback.FallbackDecorators;
import io.github.resilience4j.spring6.fallback.FallbackExecutor;
import io.github.resilience4j.spring6.fallback.ReactorFallbackDecorator;
import io.github.resilience4j.spring6.spelresolver.DefaultSpelResolver;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.reactivestreams.Publisher;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class RateLimiterAspectReactorTest {

    private RateLimiterRegistry registry;
    private RateLimiter rateLimiter;
    private TestService target;
    private TestService service;
    private final List<RateLimiterEvent> events = new ArrayList<>();

    @BeforeEach
    void setUp() {
        registry = RateLimiterRegistry.of(RateLimiterConfig.custom()
            .limitForPeriod(10)
            .limitRefreshPeriod(Duration.ofHours(1))
            .timeoutDuration(Duration.ZERO)
            .build());
        rateLimiter = registry.rateLimiter("backend");
        rateLimiter.getEventPublisher().onEvent(events::add);
        target = new TestService();
        service = proxy(target, new ReactorRateLimiterAspectExt());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldConsumeAnnotatedPermitsForEachSubscription(boolean flux) {
        Publisher<String> publisher = flux ? service.weightedFlux() : service.weightedMono();
        assertThat(rateLimiter.getMetrics().getAvailablePermissions()).isEqualTo(10);
        assertThat(target.subscriptions).hasValue(0);

        assertThat(values(publisher)).containsExactlyElementsOf(flux ? List.of("a", "b") : List.of("a"));

        assertThat(rateLimiter.getMetrics().getAvailablePermissions()).isEqualTo(7);
        assertThat(target.subscriptions).hasValue(1);

        values(publisher);

        assertThat(rateLimiter.getMetrics().getAvailablePermissions()).isEqualTo(4);
        assertThat(target.subscriptions).hasValue(2);
        assertThat(events).extracting(RateLimiterEvent::getNumberOfPermits).containsExactly(3, 3);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldRejectWithoutSubscribingWhenRemainingPermitsAreInsufficient(boolean flux) {
        assertThat(rateLimiter.acquirePermission(8)).isTrue();
        events.clear();
        Publisher<String> publisher = flux ? service.weightedFlux() : service.weightedMono();

        assertThatThrownBy(() -> values(publisher)).isInstanceOf(RequestNotPermitted.class);

        assertThat(rateLimiter.getMetrics().getAvailablePermissions()).isEqualTo(2);
        assertThat(target.subscriptions).hasValue(0);
        assertThat(events).extracting(RateLimiterEvent::getNumberOfPermits).containsExactly(3);
        assertThat(events).extracting(RateLimiterEvent::getEventType)
            .containsExactly(RateLimiterEvent.Type.FAILED_ACQUIRE);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldKeepDefaultPermitCount(boolean flux) {
        Publisher<String> publisher = flux ? service.defaultFlux() : service.defaultMono();

        values(publisher);

        assertThat(rateLimiter.getMetrics().getAvailablePermissions()).isEqualTo(9);
        assertThat(events).extracting(RateLimiterEvent::getNumberOfPermits).containsExactly(1);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldUseFallbackWhenAnnotatedPermitsAreUnavailable(boolean flux) {
        assertThat(rateLimiter.acquirePermission(8)).isTrue();
        Publisher<String> publisher = flux ? service.fluxWithFallback() : service.monoWithFallback();

        assertThat(values(publisher)).containsExactly("recovered");

        assertThat(target.subscriptions).hasValue(0);
        assertThat(rateLimiter.getMetrics().getAvailablePermissions()).isEqualTo(2);
    }

    @Test
    void shouldHonorClassLevelPermits() {
        ClassAnnotatedService classService = proxy(new ClassAnnotatedService(),
            new ReactorRateLimiterAspectExt());

        assertThat(values(classService.mono())).containsExactly("a");

        assertThat(rateLimiter.getMetrics().getAvailablePermissions()).isEqualTo(7);
    }

    @Test
    void shouldKeepExistingExtensionImplementationsWorking() {
        RateLimiterAspectExt legacyExtension = new RateLimiterAspectExt() {
            @Override
            public boolean canHandleReturnType(Class returnType) {
                return returnType == Mono.class;
            }

            @Override
            public Object handle(ProceedingJoinPoint joinPoint, RateLimiter limiter,
                String methodName) throws Throwable {
                return ((Mono<?>) joinPoint.proceed()).map(value -> "legacy:" + value);
            }
        };
        TestService legacyService = proxy(target, legacyExtension);

        assertThat(values(legacyService.weightedMono())).containsExactly("legacy:a");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldKeepLegacyReactorSubclassForDefaultPermits(boolean flux) {
        ReactorRateLimiterAspectExt legacyExtension = new ReactorRateLimiterAspectExt() {
            @Override
            public Object handle(ProceedingJoinPoint joinPoint, RateLimiter limiter,
                String methodName) throws Throwable {
                Object result = super.handle(joinPoint, limiter, methodName);
                if (result instanceof Flux<?> publisher) {
                    return publisher.map(value -> "legacy:" + value);
                }
                return ((Mono<?>) result).map(value -> "legacy:" + value);
            }
        };
        TestService legacyService = proxy(target, legacyExtension);
        Publisher<String> publisher = flux ? legacyService.defaultFlux() : legacyService.defaultMono();

        assertThat(values(publisher)).containsExactlyElementsOf(
            flux ? List.of("legacy:a", "legacy:b") : List.of("legacy:a"));

        assertThat(rateLimiter.getMetrics().getAvailablePermissions()).isEqualTo(9);
    }

    @Test
    void shouldKeepSinglePermitForExistingReactorHandleMethod() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        given(joinPoint.proceed()).willReturn(Mono.just("a"));

        Object publisher = new ReactorRateLimiterAspectExt().handle(joinPoint, rateLimiter, "mono");
        assertThat(((Mono<?>) publisher).block(Duration.ofSeconds(2))).isEqualTo("a");

        assertThat(rateLimiter.getMetrics().getAvailablePermissions()).isEqualTo(9);
    }

    private List<String> values(Publisher<String> publisher) {
        return Flux.from(publisher).collectList().block(Duration.ofSeconds(2));
    }

    private <T> T proxy(T bean, RateLimiterAspectExt extension) {
        DefaultSpelResolver spelResolver = new DefaultSpelResolver(new SpelExpressionParser(),
            new DefaultParameterNameDiscoverer(), new StaticListableBeanFactory());
        FallbackExecutor fallbackExecutor = new FallbackExecutor(spelResolver,
            new FallbackDecorators(List.of(new ReactorFallbackDecorator())));
        RateLimiterAspect aspect = new RateLimiterAspect(registry,
            new RateLimiterConfigurationProperties(), List.of(extension), fallbackExecutor, spelResolver);
        AspectJProxyFactory factory = new AspectJProxyFactory(bean);
        factory.addAspect(aspect);
        return factory.getProxy();
    }

    static class TestService {
        final AtomicInteger subscriptions = new AtomicInteger();

        @io.github.resilience4j.ratelimiter.annotation.RateLimiter(name = "backend", permits = 3)
        public Mono<String> weightedMono() {
            return sourceMono();
        }

        @io.github.resilience4j.ratelimiter.annotation.RateLimiter(name = "backend", permits = 3)
        public Flux<String> weightedFlux() {
            return sourceFlux();
        }

        @io.github.resilience4j.ratelimiter.annotation.RateLimiter(name = "backend")
        public Mono<String> defaultMono() {
            return sourceMono();
        }

        @io.github.resilience4j.ratelimiter.annotation.RateLimiter(name = "backend")
        public Flux<String> defaultFlux() {
            return sourceFlux();
        }

        @io.github.resilience4j.ratelimiter.annotation.RateLimiter(
            name = "backend", permits = 3, fallbackMethod = "recoverMono")
        public Mono<String> monoWithFallback() {
            return sourceMono();
        }

        @io.github.resilience4j.ratelimiter.annotation.RateLimiter(
            name = "backend", permits = 3, fallbackMethod = "recoverFlux")
        public Flux<String> fluxWithFallback() {
            return sourceFlux();
        }

        public Mono<String> recoverMono(RequestNotPermitted exception) {
            return Mono.just("recovered");
        }

        public Flux<String> recoverFlux(RequestNotPermitted exception) {
            return Flux.just("recovered");
        }

        private Mono<String> sourceMono() {
            return Mono.defer(() -> {
                subscriptions.incrementAndGet();
                return Mono.just("a");
            });
        }

        private Flux<String> sourceFlux() {
            return Flux.defer(() -> {
                subscriptions.incrementAndGet();
                return Flux.just("a", "b");
            });
        }
    }

    @io.github.resilience4j.ratelimiter.annotation.RateLimiter(name = "backend", permits = 3)
    static class ClassAnnotatedService {
        public Mono<String> mono() {
            return Mono.just("a");
        }
    }
}
