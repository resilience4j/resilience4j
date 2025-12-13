package io.github.resilience4j.springboot3.verifier.autoconfigure;

import org.junit.Test;

public class SpringBoot3VerifierTest {

    @Test
    public void compatibleWithCurrentSpringBoot() {
        new SpringBoot3Verifier().verifyCompatibility();
    }

}
