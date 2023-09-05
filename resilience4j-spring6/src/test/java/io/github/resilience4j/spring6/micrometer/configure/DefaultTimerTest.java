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
package io.github.resilience4j.spring6.micrometer.configure;

import io.github.resilience4j.micrometer.Timer;
import io.github.resilience4j.micrometer.TimerRegistry;
import io.github.resilience4j.spring6.TestApplication;
import io.github.resilience4j.spring6.micrometer.configure.utils.DefaultTimedService;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.concurrent.ExecutionException;

import static io.github.resilience4j.micrometer.TimerAssertions.thenFailureTimed;
import static io.github.resilience4j.micrometer.TimerAssertions.thenSuccessTimed;
import static io.github.resilience4j.spring6.micrometer.configure.utils.DefaultTimedService.BASIC_OPERATION_TIMER_NAME;
import static io.github.resilience4j.spring6.micrometer.configure.utils.DefaultTimedService.COMPLETABLE_STAGE_TIMER_NAME;
import static org.assertj.core.api.BDDAssertions.then;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = TestApplication.class)
public class DefaultTimerTest {

    @Autowired
    private MeterRegistry meterRegistry;
    @Autowired
    private TimerRegistry timerRegistry;
    @Autowired
    private DefaultTimedService service;

    @Test
    public void shouldTimeBasicOperation() {
        Timer timer = timerRegistry.timer(BASIC_OPERATION_TIMER_NAME);

        String result1 = service.succeed(123);
        thenSuccessTimed(meterRegistry, timer);
        then(result1).isEqualTo("123");

        try {
            service.fail();
        } catch (IllegalStateException e) {
            thenFailureTimed(meterRegistry, timer, e);
        }

        String result2 = service.recover(123);
        thenFailureTimed(meterRegistry, timer, new IllegalStateException());
        then(result2).isEqualTo("Basic operation recovered 123");

        Timer spelTimer = timerRegistry.timer(BASIC_OPERATION_TIMER_NAME + "SpEl");
        String result3 = service.recover(spelTimer.getName(), 123);
        thenFailureTimed(meterRegistry, spelTimer, new IllegalStateException());
        then(result3).isEqualTo("Basic operation recovered 123");
    }

    @Test
    public void shouldTimeCompletableStage() throws Throwable {
        Timer timer = timerRegistry.timer(COMPLETABLE_STAGE_TIMER_NAME);

        String result1 = service.succeedCompletionStage(123).toCompletableFuture().get();
        thenSuccessTimed(meterRegistry, timer);
        then(result1).isEqualTo("123");

        try {
            service.failCompletionStage().toCompletableFuture().get();
        } catch (ExecutionException e) {
            thenFailureTimed(meterRegistry, timer, e.getCause());
        }

        String result2 = service.recoverCompletionStage(123).toCompletableFuture().get();
        thenFailureTimed(meterRegistry, timer, new IllegalStateException());
        then(result2).isEqualTo("Completable stage recovered 123");

        Timer spelTimer = timerRegistry.timer(COMPLETABLE_STAGE_TIMER_NAME + "SpEl");
        String result3 = service.recoverCompletionStage(spelTimer.getName(), 123).toCompletableFuture().get();
        thenFailureTimed(meterRegistry, spelTimer, new IllegalStateException());
        then(result3).isEqualTo("Completable stage recovered 123");
    }
}
