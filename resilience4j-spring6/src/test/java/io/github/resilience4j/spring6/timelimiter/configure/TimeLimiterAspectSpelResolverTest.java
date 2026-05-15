/*
 * Copyright 2026 Kyuhyen Hwang
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
package io.github.resilience4j.spring6.timelimiter.configure;

import io.github.resilience4j.spring6.TestApplication;
import io.github.resilience4j.spring6.TestDummyService;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestApplication.class)
class TimeLimiterAspectSpelResolverTest {
    @Autowired
    @Qualifier("timeLimiterDummyService")
    TestDummyService testDummyService;

    @Autowired
    TimeLimiterRegistry registry;

    @Test
    void testSpel() {
        assertThat(registry.getAllTimeLimiters().stream().filter(it -> it.getName().equals("SPEL_BACKEND")).findAny().isPresent()).isFalse();
        assertThat(testDummyService.spelMono("SPEL_BACKEND").block()).isEqualTo("SPEL_BACKEND");
        assertThat(registry.getAllTimeLimiters().stream().filter(it -> it.getName().equals("SPEL_BACKEND")).findAny().isPresent()).isTrue();
    }
}
