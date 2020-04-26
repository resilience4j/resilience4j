package io.github.resilience4j.retry;

import io.github.resilience4j.retry.annotation.Retry;
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
public class RetryAnnotationMapper  implements NamedAnnotationMapper {
    @Nonnull
    @Override
    public String getName() {
        return "io.github.resilience4j.retry.annotation.Retry";
    }

    @Override
    public List<AnnotationValue<?>> map(AnnotationValue<Annotation> annotation, VisitorContext visitorContext) {
        final AnnotationValueBuilder<Retry> builder = AnnotationValue.builder(Retry.class);
        annotation.stringValue("fallbackMethod").ifPresent(s -> builder.member("fallbackMethod", s));
        AnnotationValue<Retry> ann = builder.build();
        return Collections.singletonList(ann);
    }
}
