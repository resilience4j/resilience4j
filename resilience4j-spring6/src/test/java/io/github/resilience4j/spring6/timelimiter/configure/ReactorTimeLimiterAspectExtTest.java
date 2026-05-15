package io.github.resilience4j.spring6.timelimiter.configure;

import io.github.resilience4j.timelimiter.TimeLimiter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactorTimeLimiterAspectExtTest {

    @Mock
    ProceedingJoinPoint proceedingJoinPoint;

    @InjectMocks
    ReactorTimeLimiterAspectExt reactorTimeLimiterAspectExt;

    @Test
    void testCheckTypes() {
        assertThat(reactorTimeLimiterAspectExt.canHandleReturnType(Mono.class)).isTrue();
        assertThat(reactorTimeLimiterAspectExt.canHandleReturnType(Flux.class)).isTrue();
    }

    @Test
    void testReactorTypes() throws Throwable {
        TimeLimiter timeLimiter = TimeLimiter.ofDefaults("test");

        when(proceedingJoinPoint.proceed()).thenReturn(Mono.just("Test"));
        assertThat(reactorTimeLimiterAspectExt.handle(proceedingJoinPoint, timeLimiter, "testMethod")).isNotNull();

        when(proceedingJoinPoint.proceed()).thenReturn(Flux.just("Test"));
        assertThat(reactorTimeLimiterAspectExt.handle(proceedingJoinPoint, timeLimiter, "testMethod")).isNotNull();
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWithNotReactorType() throws Throwable{
        TimeLimiter timeLimiter = TimeLimiter.ofDefaults("test");
        when(proceedingJoinPoint.proceed()).thenReturn("NOT REACTOR TYPE");

        try {
            reactorTimeLimiterAspectExt.handle(proceedingJoinPoint, timeLimiter, "testMethod");
            fail("exception missed");
        } catch (Throwable e) {
            assertThat(e).isInstanceOf(IllegalReturnTypeException.class)
                .hasMessage(
                    "java.lang.String testMethod has unsupported by @TimeLimiter return type. Reactor expects Mono/Flux.");
        }
    }
}
