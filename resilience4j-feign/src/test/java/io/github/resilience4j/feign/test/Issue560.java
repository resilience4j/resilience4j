package io.github.resilience4j.feign.test;

/**
 * <pre>
 * Caused by: java.lang.IllegalAccessException:
 * class io.github.resilience4j.feign.DefaultFallbackHandler
 * cannot access a member of class
 * io.github.resilience4j.feign.test.Issue560$$Lambda$93/0x0000000840169440 with modifiers "public"
 * </pre>
 * https://github.com/resilience4j/resilience4j/issues/560
 */
public class Issue560 {

    public static TestService createLambdaFallback() {
        return () -> "fallback";
    }
}