package io.github.resilience4j.springboot.verifier.autoconfigure;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * Displays a user-friendly message regarding resilience4j and Spring Boot version compatibility.
 */
class SpringBootVerifierFailureAnalyzer extends AbstractFailureAnalyzer<SpringBoot4Verifier.IncompatibleSpringBootVersionException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, SpringBoot4Verifier.IncompatibleSpringBootVersionException cause) {
        return new FailureAnalysis(cause.getMessage(), cause.getAction(), cause);
    }
}
