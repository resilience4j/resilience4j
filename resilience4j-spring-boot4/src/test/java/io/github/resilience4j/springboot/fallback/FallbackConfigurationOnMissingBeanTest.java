package io.github.resilience4j.springboot.fallback;

import io.github.resilience4j.spring6.fallback.FallbackDecorators;
import io.github.resilience4j.springboot.fallback.autoconfigure.FallbackConfigurationOnMissingBean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = FallbackConfigurationOnMissingBean.class)
class FallbackConfigurationOnMissingBeanTest {

    @Autowired
    private FallbackDecorators fallbackDecorators;

    @Test
    void testSizeOfDecorators() {
        assertThat(fallbackDecorators.getFallbackDecorators().size()).isEqualTo(4);
    }
}
