package io.github.resilience4j.springboot.service.test.micrometer;

import java.util.function.Function;

public class FixedOnFailureTagResolver implements Function<Throwable, String> {

    @Override
    public String apply(Throwable throwable) {
        return "error";
    }
}
