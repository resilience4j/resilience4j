package io.github.resilience4j.springboot3.service.test.micrometer;

import io.github.resilience4j.micrometer.annotation.Timer;
import io.reactivex.Single;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class TimedService {

    public static final String BASIC_TIMER_NAME = "Basic";
    public static final String REACTOR_TIMER_NAME = "Reactor";
    public static final String RXJAVA2_TIMER_NAME = "RxJava2";

    @Timer(name = BASIC_TIMER_NAME)
    public String succeedBasic(int number) {
        return String.valueOf(number);
    }

    @Timer(name = BASIC_TIMER_NAME)
    public void failBasic() {
        throw new IllegalStateException();
    }

    @Timer(name = REACTOR_TIMER_NAME)
    public Mono<String> succeedReactor(int number) {
        return Mono.just(String.valueOf(number));
    }

    @Timer(name = REACTOR_TIMER_NAME)
    public Mono<Void> failReactor() {
        return Mono.error(new IllegalStateException());
    }

    @Timer(name = RXJAVA2_TIMER_NAME)
    public Single<String> succeedRxJava2(int number) {
        return Single.just(String.valueOf(number));
    }

    @Timer(name = RXJAVA2_TIMER_NAME)
    public Single<Void> failRxJava2() {
        return Single.error(new IllegalStateException());
    }
}
