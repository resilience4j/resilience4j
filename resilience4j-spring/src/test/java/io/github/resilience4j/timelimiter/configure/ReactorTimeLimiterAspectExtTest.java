package io.github.resilience4j.timelimiter.configure;

import io.github.resilience4j.timelimiter.TimeLimiter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ReactorTimeLimiterAspectExtTest {

    @Mock
    ProceedingJoinPoint proceedingJoinPoint;

    @InjectMocks
    ReactorTimeLimiterAspectExt reactorTimeLimiterAspectExt;


    @Test
    public void testCheckTypes() {
        assertThat(reactorTimeLimiterAspectExt.canHandleReturnType(Mono.class)).isTrue();
        assertThat(reactorTimeLimiterAspectExt.canHandleReturnType(Flux.class)).isTrue();
    }

    @Test
    public void testReactorTypes() throws Throwable {
        TimeLimiter timeLimiter = TimeLimiter.ofDefaults("test");

        when(proceedingJoinPoint.proceed()).thenReturn(Mono.just("Test"));
        assertThat(reactorTimeLimiterAspectExt.handle(proceedingJoinPoint, timeLimiter, "testMethod")).isNotNull();

        when(proceedingJoinPoint.proceed()).thenReturn(Flux.just("Test"));
        assertThat(reactorTimeLimiterAspectExt.handle(proceedingJoinPoint, timeLimiter, "testMethod")).isNotNull();
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWithNotReactorType() throws Throwable{
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