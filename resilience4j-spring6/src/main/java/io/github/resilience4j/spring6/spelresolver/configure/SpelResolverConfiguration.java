/*
 * Copyright 2020 Kyuhyen Hwang
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
package io.github.resilience4j.spring6.spelresolver.configure;

import io.github.resilience4j.spring6.spelresolver.DefaultSpelResolver;
import io.github.resilience4j.spring6.spelresolver.SpelResolver;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.StandardReflectionParameterNameDiscoverer;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * {@link Configuration} for {@link SpelResolver}.
 */
@Configuration
public class SpelResolverConfiguration {
    @Bean
    public SpelResolver spelResolver(SpelExpressionParser spelExpressionParser, ParameterNameDiscoverer parameterNameDiscoverer, BeanFactory beanFactory) {
        return new DefaultSpelResolver(spelExpressionParser, parameterNameDiscoverer, beanFactory);
    }

    @Bean
    public SpelExpressionParser spelExpressionParser() {
        return new SpelExpressionParser();
    }

    /**
     * Marked {@link Primary} so that resilience4j's own {@link SpelResolver} deterministically
     * uses this bean when another library (e.g. SpringDoc) also happens to register a
     * {@link ParameterNameDiscoverer} bean of its own. Without this, the presence of two
     * same-typed beans makes the {@code parameterNameDiscoverer} parameter of
     * {@link #spelResolver} ambiguous and fails context startup with a
     * {@code NoUniqueBeanDefinitionException}, depending on auto-configuration ordering.
     */
    @Bean
    @Primary
    public ParameterNameDiscoverer parameterNameDiscoverer() {
        return new StandardReflectionParameterNameDiscoverer();
    }
}
