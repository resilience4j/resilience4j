package io.github.robwin.circuitbreaker;

public class CircuitBreakerConfig {
    private int maxFailures = 3;

    private int waitInterval = 60000;

    public CircuitBreakerConfig(){
    }

    public CircuitBreakerConfig(int maxFailures, int waitInterval){
        this.maxFailures = maxFailures;
        this.waitInterval = waitInterval;
    }

    public Integer getMaxFailures() {
        return maxFailures;
    }

    public Integer getWaitInterval() {
        return waitInterval;
    }
}
