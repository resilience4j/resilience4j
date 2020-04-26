package io.github.resilience4j.bulkhead;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
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
public class BulkheadAnnotationMapper implements NamedAnnotationMapper {
    @Nonnull
    @Override
    public String getName() {
        return "io.github.resilience4j.bulkhead.annotation.Bulkhead";
    }

    @Override
    public List<AnnotationValue<?>> map(AnnotationValue<Annotation> annotation, VisitorContext visitorContext) {
        final AnnotationValueBuilder<Bulkhead> builder = AnnotationValue.builder(Bulkhead.class).value(Bulkhead.Type.SEMAPHORE);
        annotation.enumValue("type", Bulkhead.Type.class).ifPresent(c ->
            builder.member("type", c)
        );
        annotation.stringValue("fallbackMethod").ifPresent(s -> builder.member("fallbackMethod", s));
        AnnotationValue<Bulkhead> ann = builder.build();
        return Collections.singletonList(ann);
    }
}
