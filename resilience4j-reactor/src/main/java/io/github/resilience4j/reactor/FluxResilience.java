package io.github.resilience4j.reactor;

import reactor.core.publisher.Flux;

public abstract class FluxResilience<T> extends Flux<T> {

    public static <T> Flux<T> onAssembly(Flux<T> source) {
        return Flux.onAssembly(source);
    }

}
