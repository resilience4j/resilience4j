/*
 * Copyright 2025
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
package io.github.resilience4j.springboot3.thread.autoconfigure;

import io.github.resilience4j.core.ThreadType;
import org.junit.After;
import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

public class ThreadTypeAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(Resilience4jThreadAutoConfiguration.class));

    @After
    public void tearDown() {
        System.clearProperty("resilience4j.thread.type");
    }

    @Test
    public void registersBeanWhenPropertyIsVirtual() {
        contextRunner
            .withPropertyValues("resilience4j.thread.type=virtual")
            .run(context ->
                assertThat(context).hasSingleBean(Resilience4jThreadAutoConfiguration.class)
            );
    }

    @Test
    public void doesNotRegisterBeanWhenPropertyNotSpecified() {
        contextRunner
            .withPropertyValues("randomProperty=value")
            .run(context ->
                assertThat(context).doesNotHaveBean(Resilience4jThreadAutoConfiguration.class)
            );
    }

    @Test
    public void systemPropertyIsPropagatedFromSpringProperty() {
        System.clearProperty("resilience4j.thread.type");

        contextRunner
            .withPropertyValues("resilience4j.thread.type=virtual")
            .run(context -> {
                assertThat(context).hasSingleBean(Resilience4jThreadAutoConfiguration.class);
                assertThat(System.getProperty("resilience4j.thread.type"))
                    .isEqualTo(ThreadType.VIRTUAL.toString());
            });
    }

    @Test
    public void existingSystemPropertyIsNotOverwritten() {
        System.setProperty("resilience4j.thread.type", ThreadType.PLATFORM.toString());

        contextRunner
            .withPropertyValues("resilience4j.thread.type=virtual")
            .run(context -> {
                assertThat(context).hasSingleBean(Resilience4jThreadAutoConfiguration.class);
                // The system property should remain PLATFORM (not overwritten by Spring)
                assertThat(System.getProperty("resilience4j.thread.type"))
                    .isEqualTo(ThreadType.PLATFORM.toString());
            });
    }

    @Test
    public void threadTypePropertiesBindingIsCorrect() {
        contextRunner
            .withPropertyValues("resilience4j.thread.type=virtual")
            .run(context -> {
                assertThat(context).hasSingleBean(ThreadTypeProperties.class);
                ThreadTypeProperties properties = context.getBean(ThreadTypeProperties.class);
                assertThat(properties.getType()).isEqualTo(ThreadType.VIRTUAL);
            });
    }
}
