package io.github.resilience4j.reactor.bulkhead.operator;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.reactor.FluxResilience;
import io.github.resilience4j.reactor.MonoResilience;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.function.Function;

/**
 * A Reactor operator which wraps a reactive type in a bulkhead.
 *
 * @param <T> the value type of the upstream and downstream
 */
public class BulkheadOperator<T> implements Function<Publisher<T>, Publisher<T>> {
    private final Bulkhead bulkhead;
    private final Scheduler scheduler;

    private BulkheadOperator(Bulkhead bulkhead, Scheduler scheduler) {
        this.bulkhead = bulkhead;
        this.scheduler = scheduler;
    }

    /**
     * Creates a BulkheadOperator.
     *
     * @param <T>      the value type of the upstream and downstream
     * @param bulkhead the Bulkhead
     * @return a BulkheadOperator
     */
    public static <T> BulkheadOperator<T> of(Bulkhead bulkhead) {
        return of(bulkhead, Schedulers.parallel());
    }

    /**
     * Creates a BulkheadOperator.
     *
     * @param <T>       the value type of the upstream and downstream
     * @param bulkhead  the Bulkhead
     * @param scheduler the {@link Scheduler} where to publish
     * @return a BulkheadOperator
     */
    public static <T> BulkheadOperator<T> of(Bulkhead bulkhead, Scheduler scheduler) {
        return new BulkheadOperator<>(bulkhead, scheduler);
    }

    @Override
    public Publisher<T> apply(Publisher<T> publisher) {
        if (publisher instanceof Mono) {
            return MonoResilience
                    .onAssembly(new MonoBulkhead<T>((Mono<? extends T>) publisher, bulkhead, scheduler));
        } else if (publisher instanceof Flux) {
            return FluxResilience
                    .onAssembly(new FluxBulkhead<T>((Flux<? extends T>) publisher, bulkhead, scheduler));
        }

        throw new IllegalStateException("Publisher of type <" + publisher.getClass().getSimpleName()
                + "> are not supported by this operator");
    }
}
