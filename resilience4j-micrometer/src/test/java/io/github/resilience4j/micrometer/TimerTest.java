/*
 *  Copyright 2023 Mariusz Kopylec
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
 */
package io.github.resilience4j.micrometer;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import static io.github.resilience4j.micrometer.Timer.*;
import static io.github.resilience4j.micrometer.TimerAssertions.thenFailureTimed;
import static io.github.resilience4j.micrometer.TimerAssertions.thenSuccessTimed;
import static java.util.concurrent.CompletableFuture.completedStage;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.assertj.core.api.BDDAssertions.then;

public class TimerTest {

    @Test
    public void shouldCreateDefaultTimer() {
        MeterRegistry registry = new SimpleMeterRegistry();
        Timer timer = of("timer 1", registry);

        then(timer.getName()).isEqualTo("timer 1");
        then(timer.getTags()).isEmpty();
        then(timer.getEventPublisher()).isNotNull();
        then(timer.getTimerConfig()).isNotNull();
        then(timer.getTimerConfig().getMetricNames()).isEqualTo(TimerConfig.ofDefaults().getMetricNames());
        then(timer.getTimerConfig().getOnSuccessTagResolver().get()).isEqualTo(TimerConfig.ofDefaults().getOnSuccessTagResolver().get());
        then(timer.getTimerConfig().getOnResultTagResolver().apply("123")).isEqualTo(TimerConfig.ofDefaults().getOnResultTagResolver().apply("123"));
        then(timer.getTimerConfig().getOnFailureTagResolver().apply(new IllegalStateException())).isEqualTo(TimerConfig.ofDefaults().getOnFailureTagResolver().apply(new IllegalStateException()));
    }

    @Test
    public void shouldCreateCustomTimer() {
        MeterRegistry registry = new SimpleMeterRegistry();
        TimerConfig config = TimerConfig.<String>custom()
                .metricNames("resilience4j.timer.operations")
                .onSuccessTagResolver(() -> "custom tag")
                .onResultTagResolver(result -> String.valueOf(result.length()))
                .onFailureTagResolver(throwable -> throwable.getClass().getName())
                .build();
        Map<String, String> tags = Map.of("tag 1", "value 1");
        Timer timer = of("timer 1", registry, config, tags);

        then(timer.getName()).isEqualTo("timer 1");
        then(timer.getTags()).isEqualTo(tags);
        then(timer.getEventPublisher()).isNotNull();
        then(timer.getTimerConfig()).isNotNull();
        then(timer.getTimerConfig().getMetricNames()).isEqualTo(config.getMetricNames());
        then(timer.getTimerConfig().getOnSuccessTagResolver().get()).isEqualTo(config.getOnSuccessTagResolver().get());
        then(timer.getTimerConfig().getOnResultTagResolver().apply("123")).isEqualTo(config.getOnResultTagResolver().apply("123"));
        then(timer.getTimerConfig().getOnFailureTagResolver().apply(new IllegalStateException())).isEqualTo(config.getOnFailureTagResolver().apply(new IllegalStateException()));
    }

    @Test
    public void shouldTimeSuccessfulOperationUsingDefaultTimer() throws Throwable {
        MeterRegistry registry = new SimpleMeterRegistry();
        Timer timer = of("timer 1", registry);

        String result1 = decorateSupplier(timer, () -> "result").get();
        thenSuccessTimed(registry, timer, result1);

        String result2 = decorateCheckedSupplier(timer, () -> "result").get();
        thenSuccessTimed(registry, timer, result2);

        String result3 = decorateFunction(timer, input -> "result").apply("input");
        thenSuccessTimed(registry, timer, result3);

        String result4 = decorateCheckedFunction(timer, input -> "result").apply("input");
        thenSuccessTimed(registry, timer, result4);

        String result5 = decorateCallable(timer, () -> "result").call();
        thenSuccessTimed(registry, timer, result5);

        String result6 = decorateCompletionStage(timer, () -> completedStage("result")).get().toCompletableFuture().get();
        thenSuccessTimed(registry, timer, result6);

        decorateRunnable(timer, () -> {
        }).run();
        thenSuccessTimed(registry, timer);

        decorateCheckedRunnable(timer, () -> {
        }).run();
        thenSuccessTimed(registry, timer);

        decorateConsumer(timer, input -> {
        }).accept("input");
        thenSuccessTimed(registry, timer);

        decorateCheckedConsumer(timer, input -> {
        }).accept("input");
        thenSuccessTimed(registry, timer);
    }

    @Test
    public void shouldTimeNonVoidSuccessfulOperationUsingCustomTimer() throws Throwable {
        MeterRegistry registry = new SimpleMeterRegistry();
        TimerConfig config = TimerConfig.<String>custom()
                .metricNames("resilience4j.timer.operations")
                .onResultTagResolver(result -> {
                    then(result).isEqualTo("result");
                    return result;
                })
                .build();
        Map<String, String> tags = Map.of("tag 1", "value 1");
        Timer timer = of("timer 1", registry, config, tags);

        String result1 = decorateSupplier(timer, () -> "result").get();
        thenSuccessTimed(registry, timer, result1);

        String result2 = decorateCheckedSupplier(timer, () -> "result").get();
        thenSuccessTimed(registry, timer, result2);

        String result3 = decorateFunction(timer, input -> "result").apply("input");
        thenSuccessTimed(registry, timer, result3);

        String result4 = decorateCheckedFunction(timer, input -> "result").apply("input");
        thenSuccessTimed(registry, timer, result4);

        String result5 = decorateCallable(timer, () -> "result").call();
        thenSuccessTimed(registry, timer, result5);

        String result6 = decorateCompletionStage(timer, () -> completedStage("result")).get().toCompletableFuture().get();
        thenSuccessTimed(registry, timer, result6);
    }

    @Test
    public void shouldTimeVoidSuccessfulOperationUsingCustomTimer() throws Throwable {
        MeterRegistry registry = new SimpleMeterRegistry();
        TimerConfig config = TimerConfig.custom()
                .metricNames("resilience4j.timer.operations")
                .onResultTagResolver(result -> {
                    then(result).isNull();
                    return "void";
                })
                .build();
        Map<String, String> tags = Map.of("tag 1", "value 1");
        Timer timer = of("timer 1", registry, config, tags);

        decorateRunnable(timer, () -> {
        }).run();
        thenSuccessTimed(registry, timer);

        decorateCheckedRunnable(timer, () -> {
        }).run();
        thenSuccessTimed(registry, timer);

        decorateConsumer(timer, input -> {
        }).accept("input");
        thenSuccessTimed(registry, timer);

        decorateCheckedConsumer(timer, input -> {
        }).accept("input");
        thenSuccessTimed(registry, timer);
    }

    @Test
    public void shouldTimeFailedOperationUsingDefaultTimer() throws Throwable {
        MeterRegistry registry = new SimpleMeterRegistry();
        Timer timer = of("timer 1", registry);

        try {
            decorateSupplier(timer, () -> {
                throw new IllegalStateException();
            }).get();
        } catch (IllegalStateException e) {
            thenFailureTimed(registry, timer, e);
        }

        try {
            decorateCheckedSupplier(timer, () -> {
                throw new IllegalStateException();
            }).get();
        } catch (IllegalStateException e) {
            thenFailureTimed(registry, timer, e);
        }

        try {
            decorateFunction(timer, input -> {
                throw new IllegalStateException();
            }).apply("input");
        } catch (IllegalStateException e) {
            thenFailureTimed(registry, timer, e);
        }

        try {
            decorateCheckedFunction(timer, input -> {
                throw new IllegalStateException();
            }).apply("input");
        } catch (IllegalStateException e) {
            thenFailureTimed(registry, timer, e);
        }

        try {
            decorateCallable(timer, () -> {
                throw new IllegalStateException();
            }).call();
        } catch (IllegalStateException e) {
            thenFailureTimed(registry, timer, e);
        }

        try {
            decorateCompletionStage(timer, () -> failedFuture(new IllegalStateException())).get().toCompletableFuture().get();
        } catch (ExecutionException e) {
            thenFailureTimed(registry, timer, e.getCause());
        }

        try {
            decorateRunnable(timer, () -> {
                throw new IllegalStateException();
            }).run();
        } catch (IllegalStateException e) {
            thenFailureTimed(registry, timer, e);
        }

        try {
            decorateCheckedRunnable(timer, () -> {
                throw new IllegalStateException();
            }).run();
        } catch (IllegalStateException e) {
            thenFailureTimed(registry, timer, e);
        }

        try {
            decorateConsumer(timer, input -> {
                throw new IllegalStateException();
            }).accept("input");
        } catch (IllegalStateException e) {
            thenFailureTimed(registry, timer, e);
        }

        try {
            decorateCheckedConsumer(timer, input -> {
                throw new IllegalStateException();
            }).accept("input");
        } catch (IllegalStateException e) {
            thenFailureTimed(registry, timer, e);
        }
    }

    @Test
    public void shouldTimeFailedOperationUsingCustomTimer() throws Throwable {
        MeterRegistry registry = new SimpleMeterRegistry();
        TimerConfig config = TimerConfig.custom()
                .metricNames("resilience4j.timer.operations")
                .onFailureTagResolver(throwable -> {
                    then(throwable).isInstanceOf(IllegalStateException.class);
                    return throwable.getClass().getName();
                })
                .build();
        Map<String, String> tags = Map.of("tag 1", "value 1");
        Timer timer = of("timer 1", registry, config, tags);

        try {
            decorateSupplier(timer, () -> {
                throw new IllegalStateException();
            }).get();
        } catch (IllegalStateException e) {
            thenFailureTimed(registry, timer, e);
        }

        try {
            decorateCheckedSupplier(timer, () -> {
                throw new IllegalStateException();
            }).get();
        } catch (IllegalStateException e) {
            thenFailureTimed(registry, timer, e);
        }

        try {
            decorateFunction(timer, input -> {
                throw new IllegalStateException();
            }).apply("input");
        } catch (IllegalStateException e) {
            thenFailureTimed(registry, timer, e);
        }

        try {
            decorateCheckedFunction(timer, input -> {
                throw new IllegalStateException();
            }).apply("input");
        } catch (IllegalStateException e) {
            thenFailureTimed(registry, timer, e);
        }

        try {
            decorateCallable(timer, () -> {
                throw new IllegalStateException();
            }).call();
        } catch (IllegalStateException e) {
            thenFailureTimed(registry, timer, e);
        }

        try {
            decorateCompletionStage(timer, () -> failedFuture(new IllegalStateException())).get().toCompletableFuture().get();
        } catch (ExecutionException e) {
            thenFailureTimed(registry, timer, e.getCause());
        }

        try {
            decorateRunnable(timer, () -> {
                throw new IllegalStateException();
            }).run();
        } catch (IllegalStateException e) {
            thenFailureTimed(registry, timer, e);
        }

        try {
            decorateCheckedRunnable(timer, () -> {
                throw new IllegalStateException();
            }).run();
        } catch (IllegalStateException e) {
            thenFailureTimed(registry, timer, e);
        }

        try {
            decorateConsumer(timer, input -> {
                throw new IllegalStateException();
            }).accept("input");
        } catch (IllegalStateException e) {
            thenFailureTimed(registry, timer, e);
        }

        try {
            decorateCheckedConsumer(timer, input -> {
                throw new IllegalStateException();
            }).accept("input");
        } catch (IllegalStateException e) {
            thenFailureTimed(registry, timer, e);
        }
    }
}
