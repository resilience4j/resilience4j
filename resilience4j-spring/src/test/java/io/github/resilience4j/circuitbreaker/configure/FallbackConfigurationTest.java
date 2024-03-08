package io.github.resilience4j.circuitbreaker.configure;

import io.github.resilience4j.fallback.FallbackDecorators;
import io.github.resilience4j.fallback.configure.FallbackConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = FallbackConfiguration.class)
public class FallbackConfigurationTest {

    @Autowired
    private FallbackDecorators fallbackDecorators;

    @Test
    public void testSizeOfDecorators() {
        assertThat(fallbackDecorators.getFallbackDecorators().size()).isEqualTo(4);
    }
}