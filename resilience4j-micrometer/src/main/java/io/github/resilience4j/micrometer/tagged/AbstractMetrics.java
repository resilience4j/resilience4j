package io.github.resilience4j.micrometer.tagged;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

abstract class AbstractMetrics {

    protected ConcurrentMap<String, Set<Meter.Id>> meterIdMap;

    AbstractMetrics() {
        this.meterIdMap = new ConcurrentHashMap<>();
    }

    void removeMetrics(MeterRegistry registry, String name) {
        Set<Meter.Id> ids = meterIdMap.get(name);
        if(ids != null){
            ids.forEach(registry::remove);
        }
        meterIdMap.remove(name);
    }
}
