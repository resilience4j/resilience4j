package io.github.resilience4j.springboot3.verifier.autoconfigure;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * Displays a user-friendly message regarding resilience4j and Spring Boot version compatibility.
 */
class SpringBootVerifierFailureAnalyzer extends AbstractFailureAnalyzer<SpringBoot3Verifier.IncompatibleSpringBootVersionException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, SpringBoot3Verifier.IncompatibleSpringBootVersionException cause) {
        return new FailureAnalysis(cause.getMessage(), cause.getAction(), cause);
    }
}
