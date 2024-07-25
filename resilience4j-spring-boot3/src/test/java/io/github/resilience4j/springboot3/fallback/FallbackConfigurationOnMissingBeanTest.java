package io.github.resilience4j.springboot3.fallback;

import io.github.resilience4j.springboot3.fallback.autoconfigure.FallbackConfigurationOnMissingBean;
import io.github.resilience4j.spring6.fallback.FallbackDecorators;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = FallbackConfigurationOnMissingBean.class)
public class FallbackConfigurationOnMissingBeanTest {

    @Autowired
    private FallbackDecorators fallbackDecorators;

    @Test
    public void testSizeOfDecorators() {
        assertThat(fallbackDecorators.getFallbackDecorators().size()).isEqualTo(4);
    }
}
