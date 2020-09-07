package io.github.resilience4j.common.circuitbreaker.monitoring.endpoint;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * unit test for CircuitBreakerUpdateStateResponse DTO
 */
public class CircuitBreakerUpdateStateResponseTest {

    @Test
    public void testEquals() {
        // Setup
        CircuitBreakerUpdateStateResponse response1 = new CircuitBreakerUpdateStateResponse();
        response1.setCircuitBreakerName("test2");
        response1.setCurrentState("TESTState");
        response1.setMessage("TestMessage");
        CircuitBreakerUpdateStateResponse response2 = new CircuitBreakerUpdateStateResponse();
        response2.setCircuitBreakerName("test2");
        response2.setCurrentState("TESTState");
        response2.setMessage("TestMessage");


        // Verify the results
        assertEquals(response1.getMessage(), response2.getMessage());
        assertEquals(response1.getCircuitBreakerName(), response2.getCircuitBreakerName());
        assertEquals(response1.getCurrentState(), response2.getCurrentState());
        assertEquals(response1, response2);
    }

    @Test
    public void testHashCode() {
        // Setup
        CircuitBreakerUpdateStateResponse response1 = new CircuitBreakerUpdateStateResponse();
        response1.setCircuitBreakerName("test2");
        response1.setCurrentState("TESTState");
        response1.setMessage("TestMessage");
        CircuitBreakerUpdateStateResponse response2 = new CircuitBreakerUpdateStateResponse();
        response2.setCircuitBreakerName("test2");
        response2.setCurrentState("TESTState");
        response2.setMessage("TestMessage");

        // Verify the results
        assertEquals(response1.hashCode(), response2.hashCode());
    }


    @Test
    public void testToString() {
        // Setup
        CircuitBreakerUpdateStateResponse response1 = new CircuitBreakerUpdateStateResponse();
        response1.setCircuitBreakerName("test2");
        response1.setCurrentState("TESTState");
        response1.setMessage("TestMessage");
        CircuitBreakerUpdateStateResponse response2 = new CircuitBreakerUpdateStateResponse();
        response2.setCircuitBreakerName("test2");
        response2.setCurrentState("TESTState");
        response2.setMessage("TestMessage");
        // Verify the results
        assertEquals(response1.toString(), response2.toString());
    }
}