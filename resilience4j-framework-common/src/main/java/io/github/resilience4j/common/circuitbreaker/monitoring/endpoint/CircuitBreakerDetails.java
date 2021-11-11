package io.github.resilience4j.common.circuitbreaker.monitoring.endpoint;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.core.lang.Nullable;

public class CircuitBreakerDetails {
    @Nullable
    private String failureRate;
    @Nullable
    private String slowCallRate;
    @Nullable
    private String failureRateThreshold;
    @Nullable
    private String slowCallRateThreshold;
    private int bufferedCalls;
    private int failedCalls;
    private int slowCalls;
    private int slowFailedCalls;
    private long notPermittedCalls;
    @Nullable
    private CircuitBreaker.State state;

    @Nullable
    public String getFailureRate() {
        return failureRate;
    }

    public void setFailureRate(@Nullable String failureRate) {
        this.failureRate = failureRate;
    }

    @Nullable
    public String getSlowCallRate() {
        return slowCallRate;
    }

    public void setSlowCallRate(@Nullable String slowCallRate) {
        this.slowCallRate = slowCallRate;
    }

    @Nullable
    public String getFailureRateThreshold() {
        return failureRateThreshold;
    }

    public void setFailureRateThreshold(@Nullable String failureRateThreshold) {
        this.failureRateThreshold = failureRateThreshold;
    }

    @Nullable
    public String getSlowCallRateThreshold() {
        return slowCallRateThreshold;
    }

    public void setSlowCallRateThreshold(@Nullable String slowCallRateThreshold) {
        this.slowCallRateThreshold = slowCallRateThreshold;
    }

    public int getBufferedCalls() {
        return bufferedCalls;
    }

    public void setBufferedCalls(int bufferedCalls) {
        this.bufferedCalls = bufferedCalls;
    }

    public int getFailedCalls() {
        return failedCalls;
    }

    public void setFailedCalls(int failedCalls) {
        this.failedCalls = failedCalls;
    }

    public int getSlowCalls() {
        return slowCalls;
    }

    public void setSlowCalls(int slowCalls) {
        this.slowCalls = slowCalls;
    }

    public int getSlowFailedCalls() {
        return slowFailedCalls;
    }

    public void setSlowFailedCalls(int slowFailedCalls) {
        this.slowFailedCalls = slowFailedCalls;
    }

    public long getNotPermittedCalls() {
        return notPermittedCalls;
    }

    public void setNotPermittedCalls(long notPermittedCalls) {
        this.notPermittedCalls = notPermittedCalls;
    }

    @Nullable
    public CircuitBreaker.State getState() {
        return state;
    }

    public void setState(@Nullable CircuitBreaker.State state) {
        this.state = state;
    }
}
