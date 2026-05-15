package io.github.resilience4j.spring6.circuitbreaker.configure;

import io.github.resilience4j.spring6.fallback.FallbackDecorators;
import io.github.resilience4j.spring6.fallback.configure.FallbackConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = FallbackConfiguration.class)
class FallbackConfigurationTest {

    @Autowired
    private FallbackDecorators fallbackDecorators;

    @Test
    void testSizeOfDecorators() {
        assertThat(fallbackDecorators.getFallbackDecorators().size()).isEqualTo(4);
    }
}
