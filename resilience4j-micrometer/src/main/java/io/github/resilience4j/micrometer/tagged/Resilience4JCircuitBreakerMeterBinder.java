package io.github.resilience4j.micrometer.tagged;

import io.micrometer.core.instrument.binder.MeterBinder;

/**
 * An interface to allow extendability of the default resilience4j Circuit Breaker {@link MeterBinder}
 */
public interface Resilience4JCircuitBreakerMeterBinder extends MeterBinder{
}
