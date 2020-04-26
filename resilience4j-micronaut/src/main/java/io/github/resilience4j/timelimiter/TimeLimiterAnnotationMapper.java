package io.github.resilience4j.timelimiter;

import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.AnnotationValueBuilder;
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.annotation.NamedAnnotationMapper;
import io.micronaut.inject.visitor.VisitorContext;

import javax.annotation.Nonnull;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

@Internal
public class TimeLimiterAnnotationMapper implements NamedAnnotationMapper {
    @Nonnull
    @Override
    public String getName() {
        return "io.github.resilience4j.timelimiter.annotation.TimeLimiter";
    }

    @Override
    public List<AnnotationValue<?>> map(AnnotationValue<Annotation> annotation, VisitorContext visitorContext) {
        final AnnotationValueBuilder<TimeLimiter> builder = AnnotationValue.builder(TimeLimiter.class);
        annotation.stringValue("fallbackMethod").ifPresent(c -> builder.member("fallbackMethod", c));
        AnnotationValue<TimeLimiter> ann = builder.build();
        return Collections.singletonList(ann);
    }
}
