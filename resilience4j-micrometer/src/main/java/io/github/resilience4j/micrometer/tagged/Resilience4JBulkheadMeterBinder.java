package io.github.resilience4j.micrometer.tagged;

import io.micrometer.core.instrument.binder.MeterBinder;

/**
 * An interface to allow extendability of the default resilience4j Bulkhead {@link MeterBinder}
 */
public interface Resilience4JBulkheadMeterBinder extends MeterBinder{
}
