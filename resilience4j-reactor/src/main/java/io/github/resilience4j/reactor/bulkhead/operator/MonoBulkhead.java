package io.github.resilience4j.reactor.bulkhead.operator;

import io.github.resilience4j.bulkhead.Bulkhead;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoOperator;
import reactor.core.scheduler.Scheduler;

public class MonoBulkhead<T> extends MonoOperator<T, T> {
    private final Bulkhead bulkhead;
    private final Scheduler scheduler;

    public MonoBulkhead(Mono<? extends T> source, Bulkhead bulkhead,
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
