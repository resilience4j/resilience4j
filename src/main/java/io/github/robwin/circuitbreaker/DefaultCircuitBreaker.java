package io.github.robwin.circuitbreaker;


import java.util.concurrent.atomic.AtomicReference;

/**
 * CircuitBreaker state machine.
 * <p/>
 * This CircuitBreaker is implemented via a (timed) state machine. It does not have a way to know anything about the
 * backend's state by itself, but uses only the information provided by calls to {@link #recordSuccess()} and
 * {@link #recordFailure()}.
 * <p/>
 * The state changes from CLOSED to OPEN after {@link #maxFailures} attempts have failed consecutively. Then, all access to
 * the backend is blocked for the time interval given by {@link #waitInterval}. After that, the backend is unblocked
 * tentatively, to see if it is still dead or has become available again (state: HALF_CLOSED). On success or failure, the
 * state changes back to CLOSED or OPEN, respectively.
 */
public class DefaultCircuitBreaker implements CircuitBreaker {

    private final String name;
    private final int maxFailures;
    private final int waitInterval; // milliseconds
    private AtomicReference<MonitorState> stateReference;

    /**
     * Creates a name circuitBreaker.
     *
     * @param name      the name of the CircuitBreaker
     * @param circuitBreakerConfig The CircuitBreaker configuration.
     */
    public DefaultCircuitBreaker(String name, CircuitBreakerConfig circuitBreakerConfig) {
        this.name = name;
        this.maxFailures = circuitBreakerConfig.getMaxFailures();
        this.waitInterval = circuitBreakerConfig.getWaitInterval();
        this.stateReference = new AtomicReference<>(new MonitorState(State.CLOSED, 0, 0));
    }

    /**
     * Requests permission to call this backend.
     *
     * @return true, if the call is allowed.
     */
    @Override
    public boolean isClosed() {
        MonitorState monitorState = this.stateReference.get();
        if (monitorState.getState() == State.OPEN) {
            if (System.currentTimeMillis() >= monitorState.getRetryAfter()) {
                setState(State.HALF_CLOSED, monitorState.getFailures(), 0);
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * Records a failure.
     */
    @Override
    public void recordFailure() {
        MonitorState monitorState = this.stateReference.get();
        State currentState = monitorState.getState();
        int increasedFailures = monitorState.getFailures() + 1;
        if (currentState == State.CLOSED) {
            if (increasedFailures > this.maxFailures) {
                // if CLOSED, but too many failures
                long retryAfter = System.currentTimeMillis() + this.waitInterval;
                setState(State.OPEN, increasedFailures, retryAfter);
            }else {
                // if CLOSED, only increase number of failures
                setState(monitorState.getState(), increasedFailures, monitorState.getRetryAfter());
            }
        } else if (currentState == State.HALF_CLOSED) {
            long retryAfter = System.currentTimeMillis() + this.waitInterval;
            setState(State.OPEN, increasedFailures, retryAfter);
        } else{
            // If OPEN, only increase number of failures
            setState(monitorState.getState(), increasedFailures, monitorState.getRetryAfter());
        }
    }

    /**
     * Records a success.
     */
    @Override
    public void recordSuccess() {
        setState(State.CLOSED, 0, 0);
    }

    /**
     * Get the name of the CircuitBreaker
     */
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * Get the state of the CircuitBreaker
     */
    @Override
    public State getState() {
        return this.stateReference.get().getState();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("CircuitBreaker '%s'", this.name);
    }

    private void setState(State newState, int failures, long retryAfter) {
        this.stateReference.set(new MonitorState(newState, failures, retryAfter));
    }

    static final class MonitorState {
        private final int failures;
        private final State state;
        private final long retryAfter;

        public MonitorState(State state, int failures, long retryAfter) {
            this.state = state;
            this.failures = failures;
            this.retryAfter = retryAfter;
        }

        public int getFailures() {
            return failures;
        }

        public State getState() {
            return state;
        }

        public long getRetryAfter() {
            return retryAfter;
        }
    }


}
