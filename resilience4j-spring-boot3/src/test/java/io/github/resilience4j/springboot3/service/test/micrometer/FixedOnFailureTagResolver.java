package io.github.resilience4j.springboot3.service.test.micrometer;

import java.util.function.Function;

public class FixedOnFailureTagResolver implements Function<Throwable, String> {

    @Override
    public String apply(Throwable throwable) {
        return "error";
    }
}
