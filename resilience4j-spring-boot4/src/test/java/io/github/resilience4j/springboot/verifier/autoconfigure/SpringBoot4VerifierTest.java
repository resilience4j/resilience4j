package io.github.resilience4j.springboot.verifier.autoconfigure;

import org.junit.jupiter.api.Test;

class SpringBoot4VerifierTest {

    @Test
    void compatibleWithCurrentSpringBoot() {
        new SpringBoot4Verifier().verifyCompatibility();
    }
}
