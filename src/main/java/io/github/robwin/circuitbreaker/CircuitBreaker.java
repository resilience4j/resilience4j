package io.github.robwin.circuitbreaker;

/**
 * CircuitBreaker API.
 *
 * A CircuitBreaker manages the state of a backend system. It is notified on the result of all
 * attempts to communicate with the backend, via the {@link #recordSuccess} and {@link #recordFailure} methods.
 * Before communicating with the backend, the respective connector must obtain the permission to do so via the method
 * {@link #isClosed()}.
 */
public interface CircuitBreaker {

    /**
     * Requests permission to call this circuitBreaker's backend.
     * This must be called prior to every call.
     *
     * @return true, if the call is allowed
     */
    boolean isClosed();

    /**
     * Records a backend failure.
     * This must be called if a call to this backend fails
     */
    void recordFailure();

     /**
      * Records success of a call to this backend.
      * This must be called after a successful call.
      */
    void recordSuccess();

    /**
     * Get the name of the CircuitBreaker
     */
    String getName();

    /**
     * Get the state of the CircuitBreaker
     */
    State getState();

    /**
     * States of the CircuitBreaker state machine.
     */
    enum State {
        /** A CLOSED breaker is operating normally and allowing
         requests through. */
        CLOSED,
        /** An OPEN breaker has tripped and will not allow requests
         through. */
        OPEN,
        /** A HALF_CLOSED breaker has completed its cooldown
         period and will allow one request */
        HALF_CLOSED
    }

    public static class Supplier<T> implements java.util.function.Supplier<T> {
        private final java.util.function.Supplier<T> supplier;
        private final CircuitBreaker circuitBreaker;

        public static <T> Supplier<T> of(java.util.function.Supplier<T> supplier, CircuitBreaker circuitBreaker){
            return new Supplier<>(supplier, circuitBreaker);
        }

        private Supplier(java.util.function.Supplier<T> supplier, CircuitBreaker circuitBreaker){
            this.supplier = supplier;
            this.circuitBreaker = circuitBreaker;
        }

        public T get(){
            if(!circuitBreaker.isClosed()) {
                throw new CircuitBreakerOpenException(String.format("CircuitBreaker '%s' is open", circuitBreaker.getName()));
            }
            circuitBreaker.isClosed();
            try{
                T returnValue = supplier.get();
                circuitBreaker.recordSuccess();
                return returnValue;
            } catch (Throwable throwable){
                circuitBreaker.recordFailure();
                throw throwable;
            }
        }
    }

    public static class Runnable implements java.lang.Runnable{
        private final java.lang.Runnable runnable;
        private final CircuitBreaker circuitBreaker;

        public static Runnable of(java.lang.Runnable runnable, CircuitBreaker circuitBreaker){
            return new Runnable(runnable, circuitBreaker);
        }

        private Runnable(java.lang.Runnable runnable, CircuitBreaker circuitBreaker){
            this.runnable = runnable;
            this.circuitBreaker = circuitBreaker;
        }

        public void run(){
            if(!circuitBreaker.isClosed()) {
                throw new CircuitBreakerOpenException(String.format("CircuitBreaker '%s' is open", circuitBreaker.getName()));
            }
            try{
                runnable.run();
                circuitBreaker.recordSuccess();
            } catch (Throwable throwable){
                circuitBreaker.recordFailure();
                throw throwable;
            }
        }
    }
}
