/*
 * Copyright 2025 Kyuhyen Hwang, Artur Havliukovskyi
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
package io.github.resilience4j.springboot.spelresolver.autoconfigure;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import io.github.resilience4j.spring6.spelresolver.SpelResolver;
import io.github.resilience4j.spring6.spelresolver.configure.SpelResolverConfiguration;

/**
 * {@link Configuration} for {@link SpelResolver}.
 */
@Configuration(proxyBeanMethods = false)
public class SpelResolverConfigurationOnMissingBean {

    // delegate conditional auto-configurations to regular spring configuration
    private final SpelResolverConfiguration configuration = new SpelResolverConfiguration();

    @Bean
    @ConditionalOnMissingBean
    public ParameterNameDiscoverer parameterNameDiscoverer() {
        return configuration.parameterNameDiscoverer();
    }

    @Bean
    @ConditionalOnMissingBean
    public SpelExpressionParser spelExpressionParser() {
        return configuration.spelExpressionParser();
    }

    @Bean
    @ConditionalOnMissingBean
    public SpelResolver spelResolver(SpelExpressionParser spelExpressionParser, ParameterNameDiscoverer parameterNameDiscoverer, BeanFactory beanFactory) {
        return configuration.spelResolver(spelExpressionParser, parameterNameDiscoverer, beanFactory);
    }
}
