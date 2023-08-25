package io.github.resilience4j.spring6.micrometer.configure.utils;

import java.util.function.Function;

public class FixedOnFailureTagResolver implements Function<Throwable, String> {

    @Override
    public String apply(Throwable throwable) {
        return "error";
    }
}
