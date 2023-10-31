package io.github.resilience4j.spring6.micrometer.configure.utils;

import java.util.function.Function;

public class QualifiedClassNameOnFailureTagResolver implements Function<Throwable, String> {

    @Override
    public String apply(Throwable throwable) {
        return throwable.getClass().getName();
    }
}
