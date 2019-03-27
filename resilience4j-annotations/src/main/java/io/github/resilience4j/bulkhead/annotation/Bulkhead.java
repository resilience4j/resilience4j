package io.github.resilience4j.bulkhead.annotation;

import java.lang.annotation.*;

@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.METHOD, ElementType.TYPE})
@Documented
public @interface Bulkhead {
    /**
     * Name of the bulkhead.
     *
     * @return the name of the bulkhead
     */
    String name();
}
