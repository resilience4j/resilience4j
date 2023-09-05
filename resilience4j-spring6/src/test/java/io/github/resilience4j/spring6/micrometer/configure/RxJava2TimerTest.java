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
import io.github.resilience4j.spring6.micrometer.configure.utils.RxJava2TimedService;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static io.github.resilience4j.micrometer.TimerAssertions.thenFailureTimed;
import static io.github.resilience4j.micrometer.TimerAssertions.thenSuccessTimed;
import static io.github.resilience4j.spring6.micrometer.configure.utils.RxJava2TimedService.*;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.assertj.core.api.BDDAssertions.then;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = TestApplication.class)
public class RxJava2TimerTest {

    @Autowired
    private MeterRegistry meterRegistry;
    @Autowired
    private TimerRegistry timerRegistry;
    @Autowired
    private RxJava2TimedService service;

    @Test
    public void shouldTimeCompletable() {
        Timer timer = timerRegistry.timer(COMPLETABLE_TIMER_NAME);

        Throwable result1 = service.succeedCompletable().blockingGet();
        thenSuccessTimed(meterRegistry, timer);
        then(result1).isNull();

        Throwable result2 = service.failCompletable().blockingGet();
        thenFailureTimed(meterRegistry, timer, result2);
        then(result2).isInstanceOf(IllegalStateException.class);

        Throwable result3 = service.recoverCompletable().blockingGet();
        thenFailureTimed(meterRegistry, timer, new IllegalStateException());
        then(result3).isNull();

        Timer spelTimer = timerRegistry.timer(COMPLETABLE_TIMER_NAME + "SpEl");
        Throwable result4 = service.recoverCompletable(spelTimer.getName()).blockingGet();
        thenFailureTimed(meterRegistry, spelTimer, new IllegalStateException());
        then(result4).isNull();
    }

    @Test
    public void shouldTimeSingle() {
        Timer timer = timerRegistry.timer(SINGLE_TIMER_NAME);

        String result1 = service.succeedSingle(123).blockingGet();
        thenSuccessTimed(meterRegistry, timer);
        then(result1).isEqualTo("123");

        try {
            service.failSingle().blockingGet();
            failBecauseExceptionWasNotThrown(IllegalStateException.class);
        } catch (IllegalStateException e) {
            thenFailureTimed(meterRegistry, timer, e);
        }

        String result2 = service.recoverSingle(123).blockingGet();
        thenFailureTimed(meterRegistry, timer, new IllegalStateException());
        then(result2).isEqualTo("Single recovered 123");

        Timer spelTimer = timerRegistry.timer(SINGLE_TIMER_NAME + "SpEl");
        String result3 = service.recoverSingle(spelTimer.getName(), 123).blockingGet();
        thenFailureTimed(meterRegistry, spelTimer, new IllegalStateException());
        then(result3).isEqualTo("Single recovered 123");
    }

    @Test
    public void shouldTimeMaybe() {
        Timer timer = timerRegistry.timer(MAYBE_TIMER_NAME);

        String result1 = service.succeedMaybe(123).blockingGet();
        thenSuccessTimed(meterRegistry, timer);
        then(result1).isEqualTo("123");

        try {
            service.failMaybe().blockingGet();
            failBecauseExceptionWasNotThrown(IllegalStateException.class);
        } catch (IllegalStateException e) {
            thenFailureTimed(meterRegistry, timer, e);
        }

        String result2 = service.recoverMaybe(123).blockingGet();
        thenFailureTimed(meterRegistry, timer, new IllegalStateException());
        then(result2).isEqualTo("Maybe recovered 123");

        Timer spelTimer = timerRegistry.timer(MAYBE_TIMER_NAME + "SpEl");
        String result3 = service.recoverMaybe(spelTimer.getName(), 123).blockingGet();
        thenFailureTimed(meterRegistry, spelTimer, new IllegalStateException());
        then(result3).isEqualTo("Maybe recovered 123");
    }

    @Test
    public void shouldTimeObservable() {
        Timer timer = timerRegistry.timer(OBSERVABLE_TIMER_NAME);

        List<String> result1 = service.succeedObservable(123).toList().blockingGet();
        thenSuccessTimed(meterRegistry, timer);
        then(result1).containsExactly("123");

        try {
            service.failObservable().toList().blockingGet();
            failBecauseExceptionWasNotThrown(IllegalStateException.class);
        } catch (IllegalStateException e) {
            thenFailureTimed(meterRegistry, timer, e);
        }

        List<String> result2 = service.recoverObservable(123).toList().blockingGet();
        thenFailureTimed(meterRegistry, timer, new IllegalStateException());
        then(result2).containsExactly("Observable recovered 123");

        Timer spelTimer = timerRegistry.timer(OBSERVABLE_TIMER_NAME + "SpEl");
        List<String> result3 = service.recoverObservable(spelTimer.getName(), 123).toList().blockingGet();
        thenFailureTimed(meterRegistry, spelTimer, new IllegalStateException());
        then(result3).containsExactly("Observable recovered 123");
    }

    @Test
    public void shouldTimeFlowable() {
        Timer timer = timerRegistry.timer(FLOWABLE_TIMER_NAME);

        List<String> result1 = service.succeedFlowable(123).toList().blockingGet();
        thenSuccessTimed(meterRegistry, timer);
        then(result1).containsExactly("123");

        try {
            service.failFlowable().toList().blockingGet();
            failBecauseExceptionWasNotThrown(IllegalStateException.class);
        } catch (IllegalStateException e) {
            thenFailureTimed(meterRegistry, timer, e);
        }

        List<String> result2 = service.recoverFlowable(123).toList().blockingGet();
        thenFailureTimed(meterRegistry, timer, new IllegalStateException());
        then(result2).containsExactly("Flowable recovered 123");

        Timer spelTimer = timerRegistry.timer(FLOWABLE_TIMER_NAME + "SpEl");
        List<String> result3 = service.recoverFlowable(spelTimer.getName(), 123).toList().blockingGet();
        thenFailureTimed(meterRegistry, spelTimer, new IllegalStateException());
        then(result3).containsExactly("Flowable recovered 123");
    }
}
