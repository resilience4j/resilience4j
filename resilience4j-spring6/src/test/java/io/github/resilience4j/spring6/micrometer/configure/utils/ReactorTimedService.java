package io.github.resilience4j.spring6.micrometer.configure.utils;

import io.github.resilience4j.micrometer.annotation.Timer;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class ReactorTimedService {

    public static final String MONO_TIMER_NAME = "Mono";
    public static final String FLUX_TIMER_NAME = "Flux";

    @Timer(name = MONO_TIMER_NAME)
    public Mono<String> succeedMono(int number) {
        return Mono.just(String.valueOf(number));
    }

    @Timer(name = MONO_TIMER_NAME)
    public Mono<Void> failMono() {
        return Mono.error(new IllegalStateException());
    }

    @Timer(name = MONO_TIMER_NAME, fallbackMethod = "fallbackMono")
    public Mono<String> recoverMono(int number) {
        return Mono.error(new IllegalStateException("Mono recovered " + number));
    }

    @Timer(name = "#root.args[0]", fallbackMethod = "${missing.property:fallbackMono}")
    public Mono<String> recoverMono(String timerName, int number) {
        return Mono.error(new IllegalStateException("Mono recovered " + number));
    }

    @Timer(name = FLUX_TIMER_NAME)
    public Flux<String> succeedFlux(int number) {
        return Flux.just(String.valueOf(number));
    }

    @Timer(name = FLUX_TIMER_NAME)
    public Flux<Void> failFlux() {
        return Flux.error(new IllegalStateException());
    }

    @Timer(name = FLUX_TIMER_NAME, fallbackMethod = "fallbackFlux")
    public Flux<String> recoverFlux(int number) {
        return Flux.error(new IllegalStateException("Flux recovered " + number));
    }

    @Timer(name = "#root.args[0]", fallbackMethod = "${missing.property:fallbackFlux}")
    public Flux<String> recoverFlux(String timerName, int number) {
        return Flux.error(new IllegalStateException("Flux recovered " + number));
    }

    private Mono<String> fallbackMono(IllegalStateException e) {
        return Mono.just(e.getMessage());
    }

    private Flux<String> fallbackFlux(IllegalStateException e) {
        return Flux.just(e.getMessage());
    }
}
