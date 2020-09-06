/*
 *
 *  Copyright 2020: KrnSaurabh
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package io.github.resilience4j.metrics;

import com.codahale.metrics.MetricRegistry;
import io.github.resilience4j.test.HelloWorldService;
import io.vavr.CheckedFunction0;
import io.vavr.CheckedFunction1;
import io.vavr.CheckedRunnable;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

public class VavrTimerTest {

    private HelloWorldService helloWorldService;
    private Timer timer;
    private MetricRegistry metricRegistry;

    @Before
    public void setUp() {
        metricRegistry = new MetricRegistry();
        timer = Timer.ofMetricRegistry(VavrTimerTest.class.getName(), metricRegistry);
        helloWorldService = mock(HelloWorldService.class);
    }

    @Test
    public void shouldDecorateCheckedSupplier() throws Throwable {
        given(helloWorldService.returnHelloWorldWithException()).willReturn("Hello world");
        CheckedFunction0<String> timedSupplier = VavrTimer
            .decorateCheckedSupplier(timer, helloWorldService::returnHelloWorldWithException);

        String value = timedSupplier.apply();

        assertThat(timer.getMetrics().getNumberOfTotalCalls()).isEqualTo(1);
        assertThat(timer.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(timer.getMetrics().getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(metricRegistry.getCounters().size()).isEqualTo(2);
        assertThat(metricRegistry.getTimers().size()).isEqualTo(1);
        assertThat(value).isEqualTo("Hello world");
        then(helloWorldService).should(times(1)).returnHelloWorldWithException();
    }

    @Test
    public void shouldDecorateCheckedRunnableAndReturnWithSuccess() throws Throwable {
        CheckedRunnable timedRunnable = VavrTimer
            .decorateCheckedRunnable(timer, helloWorldService::sayHelloWorldWithException);

        timedRunnable.run();

        assertThat(timer.getMetrics().getNumberOfTotalCalls()).isEqualTo(1);
        assertThat(timer.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(timer.getMetrics().getNumberOfFailedCalls()).isEqualTo(0);
        then(helloWorldService).should().sayHelloWorldWithException();
    }

    @Test
    public void shouldDecorateCheckedFunctionAndReturnWithSuccess() throws Throwable {
        given(helloWorldService.returnHelloWorldWithNameWithException("Tom"))
            .willReturn("Hello world Tom");
        CheckedFunction1<String, String> function = VavrTimer.decorateCheckedFunction(timer,
            helloWorldService::returnHelloWorldWithNameWithException);

        String result = function.apply("Tom");

        assertThat(result).isEqualTo("Hello world Tom");
        assertThat(timer.getMetrics().getNumberOfTotalCalls()).isEqualTo(1);
        assertThat(timer.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(timer.getMetrics().getNumberOfFailedCalls()).isEqualTo(0);
        then(helloWorldService).should().returnHelloWorldWithNameWithException("Tom");
    }
}
