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
package io.github.resilience4j.springboot.fallback.autoconfigure;

import io.github.resilience4j.spring6.fallback.*;
import io.github.resilience4j.spring6.fallback.configure.FallbackConfiguration;
import io.github.resilience4j.spring6.spelresolver.SpelResolver;
import io.github.resilience4j.spring6.utils.AspectJOnClasspathCondition;
import io.github.resilience4j.spring6.utils.ReactorOnClasspathCondition;
import io.github.resilience4j.spring6.utils.RxJava2OnClasspathCondition;
import io.github.resilience4j.spring6.utils.RxJava3OnClasspathCondition;
import io.github.resilience4j.springboot.spelresolver.autoconfigure.SpelResolverConfigurationOnMissingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.List;

/**
 * {@link Configuration} for {@link FallbackDecorators}.
 */
@Configuration(proxyBeanMethods = false)
@Import(SpelResolverConfigurationOnMissingBean.class)
public class FallbackConfigurationOnMissingBean {

    // delegate conditional auto-configurations to regular spring configuration
    private final FallbackConfiguration fallbackConfiguration = new FallbackConfiguration();

    @Bean
    @ConditionalOnMissingBean
    @Conditional(value = {AspectJOnClasspathCondition.class})
    public FallbackDecorators fallbackDecorators(@Autowired(required = false) List<FallbackDecorator> fallbackDecorators) {
        return fallbackConfiguration.fallbackDecorators(fallbackDecorators);
    }

    @Bean
    @ConditionalOnMissingBean
    @Conditional(value = {AspectJOnClasspathCondition.class})
    public FallbackExecutor fallbackExecutor(SpelResolver spelResolver, FallbackDecorators fallbackDecorators) {
        return fallbackConfiguration.fallbackExecutor(spelResolver, fallbackDecorators);
    }

    @Bean
    @Conditional(value = {RxJava2OnClasspathCondition.class})
    @ConditionalOnMissingBean
    public RxJava2FallbackDecorator rxJava2FallbackDecorator() {
        return fallbackConfiguration.rxJava2FallbackDecorator();
    }

    @Bean
    @Conditional(value = {RxJava3OnClasspathCondition.class})
    @ConditionalOnMissingBean
    public RxJava3FallbackDecorator rxJava3FallbackDecorator() {
        return fallbackConfiguration.rxJava3FallbackDecorator();
    }

    @Bean
    @Conditional(value = {ReactorOnClasspathCondition.class})
    @ConditionalOnMissingBean
    public ReactorFallbackDecorator reactorFallbackDecorator() {
        return fallbackConfiguration.reactorFallbackDecorator();
    }

    @Bean
    @ConditionalOnMissingBean
    public CompletionStageFallbackDecorator completionStageFallbackDecorator() {
        return fallbackConfiguration.completionStageFallbackDecorator();
    }
}
