package io.github.resilience4j.utils;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AnnotationExtractorTest {

    @Test
    public void testExtract() {
        CircuitBreaker circuitBreaker = AnnotationExtractor
            .extract(AnnotatedClass.class, CircuitBreaker.class);

        assertThat(circuitBreaker).isNotNull();
        assertThat(circuitBreaker.name()).isEqualTo("test");
    }

    @Test
    public void testExtract2() {
        CircuitBreaker circuitBreaker = AnnotationExtractor
            .extract(NotAnnotatedClass.class, CircuitBreaker.class);

        assertThat(circuitBreaker).isNull();
    }

    @CircuitBreaker(name = "test")
    private static class AnnotatedClass {

        public void withAnnotation() {
        }
    }

    private static class NotAnnotatedClass {

        public void withAnnotation() {
        }
    }
}