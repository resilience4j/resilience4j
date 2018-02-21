package io.github.resilience4j.reactor.bulkhead.operator;

import io.github.resilience4j.bulkhead.Bulkhead;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxOperator;
import reactor.core.scheduler.Scheduler;

public class FluxBulkhead<T> extends FluxOperator<T, T> {

    private final Bulkhead bulkhead;
    private final Scheduler scheduler;

    public FluxBulkhead(Flux<? extends T> source, Bulkhead bulkhead,
                        Scheduler scheduler) {
        super(source);
        this.bulkhead = bulkhead;
        this.scheduler = scheduler;
    }

    @Override
    public void subscribe(CoreSubscriber<? super T> actual) {
        source.publishOn(scheduler)
                .subscribe(new BulkheadSubscriber<>(bulkhead, actual));
    }

}