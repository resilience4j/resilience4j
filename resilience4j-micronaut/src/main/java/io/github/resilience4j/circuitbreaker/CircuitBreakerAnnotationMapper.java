package io.github.resilience4j.circuitbreaker;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.AnnotationValueBuilder;
import io.micronaut.inject.annotation.NamedAnnotationMapper;
import io.micronaut.inject.visitor.VisitorContext;

import javax.annotation.Nonnull;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

public class CircuitBreakerAnnotationMapper implements NamedAnnotationMapper {
    @Nonnull
    @Override
    public String getName() {
        return "io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker";
    }

    @Override
    public List<AnnotationValue<?>> map(AnnotationValue<Annotation> annotation, VisitorContext visitorContext) {
        final AnnotationValueBuilder<CircuitBreaker> builder = AnnotationValue.builder(CircuitBreaker.class);
        annotation.stringValue("fallbackMethod").ifPresent(c -> builder.member("fallbackMethod", c));
        AnnotationValue<CircuitBreaker> ann = builder.build();
        return Collections.singletonList(ann);
    }
}
