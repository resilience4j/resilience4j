package io.github.resilience4j.springboot3.service.test.micrometer;

import java.util.function.Function;

public class QualifiedClassNameOnFailureTagResolver implements Function<Throwable, String> {

    @Override
    public String apply(Throwable throwable) {
        return throwable.getClass().getName();
    }
}
