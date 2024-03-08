/*
 * Copyright 2019 Kyuhyen Hwang , Mahmoud Romeh
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
package io.github.resilience4j.spring6.fallback.configure;

import io.github.resilience4j.spring6.fallback.*;
import io.github.resilience4j.spring6.spelresolver.SpelResolver;
import io.github.resilience4j.spring6.spelresolver.configure.SpelResolverConfiguration;
import io.github.resilience4j.spring6.utils.ReactorOnClasspathCondition;
import io.github.resilience4j.spring6.utils.RxJava2OnClasspathCondition;
import io.github.resilience4j.spring6.utils.RxJava3OnClasspathCondition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.List;

/**
 * {@link Configuration} for {@link FallbackDecorators}.
 */
@Configuration
@Import(SpelResolverConfiguration.class)
public class FallbackConfiguration {

    @Bean
    @Conditional(value = {RxJava2OnClasspathCondition.class})
    public RxJava2FallbackDecorator rxJava2FallbackDecorator() {
        return new RxJava2FallbackDecorator();
    }

    @Bean
    @Conditional(value = {RxJava3OnClasspathCondition.class})
    public RxJava3FallbackDecorator rxJava3FallbackDecorator() {
        return new RxJava3FallbackDecorator();
    }

    @Bean
    @Conditional(value = {ReactorOnClasspathCondition.class})
    public ReactorFallbackDecorator reactorFallbackDecorator() {
        return new ReactorFallbackDecorator();
    }

    @Bean
    public CompletionStageFallbackDecorator completionStageFallbackDecorator() {
        return new CompletionStageFallbackDecorator();
    }

    @Bean
    public FallbackDecorators fallbackDecorators(@Autowired(required = false) List<FallbackDecorator> fallbackDecorator) {
        return new FallbackDecorators(fallbackDecorator);
    }

    @Bean
    public FallbackExecutor fallbackExecutor(SpelResolver spelResolver, FallbackDecorators fallbackDecorators) {
        return new FallbackExecutor(spelResolver, fallbackDecorators);
    }
}
