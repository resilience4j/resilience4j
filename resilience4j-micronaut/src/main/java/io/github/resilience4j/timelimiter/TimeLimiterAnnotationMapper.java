/*
 * Copyright 2019 Michael Pollind
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.timelimiter;

import io.github.resilience4j.annotation.TimeLimiter;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.AnnotationValueBuilder;
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.annotation.NamedAnnotationMapper;
import io.micronaut.inject.visitor.VisitorContext;

import javax.annotation.Nonnull;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

/**
 * An annotation mapper that maps {@link TimeLimiter}.
 */
@Internal
public class TimeLimiterAnnotationMapper implements NamedAnnotationMapper {
    @Nonnull
    @Override
    public String getName() {
        return "io.github.resilience4j.annotation.TimeLimiter";
    }

    @Override
    public List<AnnotationValue<?>> map(AnnotationValue<Annotation> annotation, VisitorContext visitorContext) {
        final AnnotationValueBuilder<TimeLimiter> builder = AnnotationValue.builder(TimeLimiter.class);
        annotation.stringValue("fallbackMethod").ifPresent(c -> builder.member("fallbackMethod", c));
        AnnotationValue<TimeLimiter> ann = builder.build();
        return Collections.singletonList(ann);
    }
}
