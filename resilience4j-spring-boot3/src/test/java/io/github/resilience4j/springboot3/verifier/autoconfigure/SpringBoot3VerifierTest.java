package io.github.resilience4j.springboot3.verifier.autoconfigure;

import org.junit.jupiter.api.Test;

class SpringBoot3VerifierTest {

    @Test
    void compatibleWithCurrentSpringBoot() {
        new SpringBoot3Verifier().verifyCompatibility();
    }
}
