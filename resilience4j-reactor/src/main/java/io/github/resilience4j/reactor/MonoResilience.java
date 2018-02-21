package io.github.resilience4j.reactor;

import reactor.core.publisher.Mono;

public abstract class MonoResilience<T> extends Mono<T> {

    public static <T> Mono<T> onAssembly(Mono<T> source) {
        return Mono.onAssembly(source);
    }
}
