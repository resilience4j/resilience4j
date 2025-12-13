package io.github.resilience4j.springboot3.verifier.autoconfigure;

import org.springframework.boot.SpringBootVersion;

/**
 * Verifies compatibility with Spring Boot 3. Class name intentionally contains the major version in case of possible
 * classpath overriding issues.
 */
public class SpringBoot3Verifier {
    public void verifyCompatibility() {
        var springBootMajorVersion = Integer.parseInt(SpringBootVersion.getVersion().split("\\.")[0]);
        if (springBootMajorVersion == 4) {
            throw new IncompatibleSpringBootVersionException("Module 'io.github.resilience4j:resilience4j-spring-boot3' is only compatible with Spring Boot 3.x", "Update your project to use 'io.github.resilience4j:resilience4j-spring-boot4'");
        } else if (springBootMajorVersion != 3) {
            throw new IncompatibleSpringBootVersionException("Module 'io.github.resilience4j:resilience4j-spring-boot3' is only compatible with Spring Boot 3.x", "Update your project to use compatible spring boot module");
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
