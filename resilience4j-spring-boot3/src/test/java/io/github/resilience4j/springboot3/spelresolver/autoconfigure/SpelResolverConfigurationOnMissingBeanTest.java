/*
 * Copyright 2026 Sanskar Shimpi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.springboot3.spelresolver.autoconfigure;

import io.github.resilience4j.spring6.spelresolver.SpelResolver;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reproduces the bean ambiguity reported for resilience4j + SpringDoc (and any other library
 * that registers its own {@link ParameterNameDiscoverer} bean): when such a competing bean is
 * only registered in the application context after
 * {@link SpelResolverConfigurationOnMissingBean} has already been processed,
 * {@code @ConditionalOnMissingBean} does not see it in time, so resilience4j's own
 * {@code parameterNameDiscoverer} bean is created anyway. Both beans then coexist, and the
 * {@code spelResolver} bean, which previously auto-wired a plain (non-qualified)
 * {@link ParameterNameDiscoverer}, would fail to start with a
 * {@code NoUniqueBeanDefinitionException}.
 */
class SpelResolverConfigurationOnMissingBeanTest {

    @Test
    void contextStartsWhenAnotherParameterNameDiscovererBeanIsAlsoPresent() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        // Registration order matters here: SpelResolverConfigurationOnMissingBean must be
        // registered (and its @ConditionalOnMissingBean evaluated) before the competing bean
        // definition is known to the context, to reproduce the real-world ordering issue.
        context.register(SpelResolverConfigurationOnMissingBean.class, CompetingParameterNameDiscovererConfig.class);
        context.refresh();

        assertThat(context.getBeansOfType(ParameterNameDiscoverer.class)).hasSize(2);
        // Resolved purely by type (no parameter-name tie-break involved): this only succeeds
        // if exactly one of the two candidates is marked @Primary.
        assertThat(context.getBean(ParameterNameDiscoverer.class)).isNotNull();
        assertThat(context.getBean(SpelResolver.class)).isNotNull();

        context.close();
    }

    @Configuration
    static class CompetingParameterNameDiscovererConfig {

        // Simulates a third-party library (e.g. SpringDoc's
        // localSpringDocParameterNameDiscoverer bean) that registers its own
        // ParameterNameDiscoverer bean without any awareness of resilience4j's bean.
        @Bean
        public ParameterNameDiscoverer anotherParameterNameDiscoverer() {
            return new DefaultParameterNameDiscoverer();
        }
    }
}
