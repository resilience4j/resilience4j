/*
 * Copyright 2019 Mahmoud Romeh
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
package io.github.resilience4j.retry.configure;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import io.github.resilience4j.retry.Retry;
import io.vavr.CheckedFunction0;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * aspect unit test
 */
@RunWith(MockitoJUnitRunner.class)
public class ReactorRetryAspectExtTest {

    @InjectMocks
    ReactorRetryAspectExt reactorRetryAspectExt;

    @Test
    public void testCheckTypes() {
        assertThat(reactorRetryAspectExt.canHandleReturnType(Mono.class)).isTrue();
        assertThat(reactorRetryAspectExt.canHandleReturnType(Flux.class)).isTrue();
    }

    @Test
    public void testMonoType() throws Throwable {
        Retry retry = Retry.ofDefaults("test");
        CheckedFunction0<Object> decorated = reactorRetryAspectExt.decorate(retry, () -> Mono.just("Test"));
        assertThat(decorated.apply()).isInstanceOf(Mono.class);
    }

    @Test
    public void testFluxType() throws Throwable {
        Retry retry = Retry.ofDefaults("test");
        CheckedFunction0<Object> decorated = reactorRetryAspectExt.decorate(retry, () -> Flux.just("Test"));
        assertThat(decorated.apply()).isInstanceOf(Flux.class);
    }
}
