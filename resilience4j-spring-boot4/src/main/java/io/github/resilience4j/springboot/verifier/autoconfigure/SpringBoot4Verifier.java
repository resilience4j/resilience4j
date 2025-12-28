package io.github.resilience4j.springboot.verifier.autoconfigure;

import org.springframework.boot.SpringBootVersion;

/**
 * Verifies compatibility with Spring Boot 4. Class name intentionally contains the major version in case of possible
 * classpath overriding issues.
 */
public class SpringBoot4Verifier {
    public void verifyCompatibility() {
        var springBootMajorVersion = Integer.parseInt(SpringBootVersion.getVersion().split("\\.")[0]);
        if (springBootMajorVersion != 4) {
            throw new IncompatibleSpringBootVersionException("Module 'io.github.resilience4j:resilience4j-spring-boot4' is only compatible with Spring Boot 4.x", "Update your project to use compatible Spring Boot module");
        }
    }

    public static class IncompatibleSpringBootVersionException extends RuntimeException {
        private final String action;

        public IncompatibleSpringBootVersionException(String message, String action) {
            super(message);
            this.action = action;
        }

        public String getAction() {
            return action;
        }
    }
}
