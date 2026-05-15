package io.github.resilience4j.spring6.utils;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AnnotationExtractorTest {

    @Test
    void testExtract() {
        CircuitBreaker circuitBreaker = AnnotationExtractor
            .extract(AnnotatedClass.class, CircuitBreaker.class);

        assertThat(circuitBreaker).isNotNull();
        assertThat(circuitBreaker.name()).isEqualTo("test");
    }

    @Test
    void testExtract2() {
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
