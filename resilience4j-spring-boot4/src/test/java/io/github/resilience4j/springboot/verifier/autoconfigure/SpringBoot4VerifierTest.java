package io.github.resilience4j.springboot.verifier.autoconfigure;

import org.junit.Test;

public class SpringBoot4VerifierTest {

    @Test
    public void compatibleWithCurrentSpringBoot() {
        new SpringBoot4Verifier().verifyCompatibility();
    }
}
