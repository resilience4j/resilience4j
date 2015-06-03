package io.github.robwin.circuitbreaker;

import org.junit.Before;
import org.junit.Test;

import static java.lang.Thread.sleep;
import static org.assertj.core.api.BDDAssertions.assertThat;

public class CircuitBreakerTest {

    private CircuitBreaker circuitBreaker;

    @Before
    public void setUp(){
        circuitBreaker = new DefaultCircuitBreaker("testName",  new CircuitBreakerConfig(2, 1000));
    }

    @Test
    public void shouldReturnTheCorrectName(){
        assertThat(circuitBreaker.toString()).isEqualTo("CircuitBreaker 'testName'");
    }

    @Test
    public void testCircuitBreaker() throws InterruptedException {
        assertThat(circuitBreaker.isClosed()).isEqualTo(true);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        circuitBreaker.recordFailure();  // failure 1
        assertThat(circuitBreaker.isClosed()).isEqualTo(true);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        circuitBreaker.recordFailure();  // failure 2
        assertThat(circuitBreaker.isClosed()).isEqualTo(true);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        circuitBreaker.recordFailure();  // failure 3
        assertThat(circuitBreaker.isClosed()).isEqualTo(false);  // open after third failure
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        sleep(500);
        assertThat(circuitBreaker.isClosed()).isEqualTo(false); // still open
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN); // still open
        sleep(800);
        assertThat(circuitBreaker.isClosed()).isEqualTo(true); // half-closed after more than 1 second
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_CLOSED);
        circuitBreaker.recordFailure(); // but backend still unavailable
        assertThat(circuitBreaker.isClosed()).isEqualTo(false); // back to open
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        sleep(1300);
        assertThat(circuitBreaker.isClosed()).isEqualTo(true); // half-closed after more than 1 second
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_CLOSED);
        circuitBreaker.recordSuccess();
        assertThat(circuitBreaker.isClosed()).isEqualTo(true);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED); // closed again and failure count is reset
        circuitBreaker.recordFailure();
        assertThat(circuitBreaker.isClosed()).isEqualTo(true);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);  // closed because failure count was reset
    }
}
