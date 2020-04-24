package io.github.resilience4j.timelimiter;

import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.inject.annotation.NamedAnnotationMapper;
import io.micronaut.inject.visitor.VisitorContext;

import javax.annotation.Nonnull;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

public class TimeLimiterAnnotationMapper implements NamedAnnotationMapper {
    @Nonnull
    @Override
    public String getName() {
        return "io.github.resilience4j.timelimiter.annotation.TimeLimiter";
    }

    @Override
    public List<AnnotationValue<?>> map(AnnotationValue<Annotation> annotation, VisitorContext visitorContext) {
        return Collections.singletonList(AnnotationValue.builder(TimeLimiter.class).build());
    }
}
