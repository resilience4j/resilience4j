/*
 *
 *  Copyright 2017 Christopher Pilsworth
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
package io.github.resilience4j.retrofit;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Tests the integration of the Retrofit HTTP client and {@link CircuitBreaker}
 */
public class RetrofitCircuitBreakerTest {

    private static final CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
        .slidingWindowSize(3)
        .waitDurationInOpenState(Duration.ofMillis(150))
        .build();
    @Rule
    public WireMockRule wireMockRule = new WireMockRule();
    private CircuitBreaker circuitBreaker;
    private OkHttpClient client;
    private RetrofitService service;

    @Before
    public void setUp() {
        this.circuitBreaker = CircuitBreaker.of("test", circuitBreakerConfig);

        final long TIMEOUT = 150; // ms
        this.client = new OkHttpClient.Builder()
            .connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
            .readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
            .writeTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
            .build();

        this.service = new Retrofit.Builder()
            .addCallAdapterFactory(CircuitBreakerCallAdapter.of(circuitBreaker))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(ScalarsConverterFactory.create())
            .baseUrl(wireMockRule.baseUrl())
            .client(client)
            .build()
            .create(RetrofitService.class);
    }

    @Test
    public void decorateSuccessfulCall() throws Exception {
        stubFor(get(urlPathEqualTo("/greeting"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/plain")
                .withBody("hello world")));

        service.greeting().execute();

        verify(1, getRequestedFor(urlPathEqualTo("/greeting")));
    }

    @Test
    public void decorateSuccessfulEnqueuedCall() throws Throwable {
        stubFor(get(urlPathEqualTo("/greeting"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/plain")
                .withBody("hello world")));

        EnqueueDecorator.enqueue(service.greeting());

        verify(1, getRequestedFor(urlPathEqualTo("/greeting")));
    }

    @Test
    public void decorateTimingOutCall() throws Exception {
        stubFor(get(urlPathEqualTo("/greeting"))
            .willReturn(aResponse()
                .withFixedDelay(500)
                .withStatus(200)
                .withHeader("Content-Type", "text/plain")
                .withBody("hello world")));

        try {
            service.greeting().execute();
        } catch (Throwable ignored) {
        }

        final CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfFailedCalls())
            .describedAs("Failed calls")
            .isEqualTo(1);

        // Circuit breaker should still be closed, not hit open threshold
        assertThat(circuitBreaker.getState())
            .isEqualTo(CircuitBreaker.State.CLOSED);

        try {
            service.greeting().execute();
        } catch (Throwable ignored) {
        }

        try {
            service.greeting().execute();
        } catch (Throwable ignored) {
        }

        assertThat(metrics.getNumberOfFailedCalls())
            .isEqualTo(3);
        // Circuit breaker should be OPEN, threshold met
        assertThat(circuitBreaker.getState())
            .isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    public void decorateCancelledCall() throws Exception {
        stubFor(get(urlPathEqualTo("/greeting"))
            .willReturn(aResponse()
                .withFixedDelay(500)
                .withStatus(200)
                .withHeader("Content-Type", "text/plain")
                .withBody("hello world")));

        try {
            Call<String> call = service.greeting();
            cancelAsync(call, 100);
            call.execute();
        } catch (Throwable ignored) {
        }

        final CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfFailedCalls())
            .describedAs("Failed calls")
            .isEqualTo(0);

        // Circuit breaker should still be closed, not hit open threshold
        assertThat(circuitBreaker.getState())
            .isEqualTo(CircuitBreaker.State.CLOSED);

        try {
            service.greeting().execute();
        } catch (Throwable ignored) {
        }

        try {
            service.greeting().execute();
        } catch (Throwable ignored) {
        }

        try {
            service.greeting().execute();
        } catch (Throwable ignored) {
        }

        assertThat(metrics.getNumberOfFailedCalls())
            .isEqualTo(3);
        // Circuit breaker should be OPEN, threshold met
        assertThat(circuitBreaker.getState())
            .isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    public void decorateCancelledEnqueuedCall() throws Exception {
        stubFor(get(urlPathEqualTo("/greeting"))
            .willReturn(aResponse()
                .withFixedDelay(500)
                .withStatus(200)
                .withHeader("Content-Type", "text/plain")
                .withBody("hello world")));

        Call<String> call = service.greeting();
        cancelAsync(call, 100);
        EnqueueDecorator.performCatchingEnqueue(call);

        final CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfFailedCalls())
            .describedAs("Failed calls")
            .isEqualTo(0);

        // Circuit breaker should still be closed, not hit open threshold
        assertThat(circuitBreaker.getState())
            .isEqualTo(CircuitBreaker.State.CLOSED);

        EnqueueDecorator.performCatchingEnqueue(service.greeting());
        EnqueueDecorator.performCatchingEnqueue(service.greeting());
        EnqueueDecorator.performCatchingEnqueue(service.greeting());

        assertThat(metrics.getNumberOfFailedCalls())
            .isEqualTo(3);
        // Circuit breaker should be OPEN, threshold met
        assertThat(circuitBreaker.getState())
            .isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    public void decorateTimingOutEnqueuedCall() throws Exception {
        stubFor(get(urlPathEqualTo("/greeting"))
            .willReturn(aResponse()
                .withFixedDelay(500)
                .withStatus(200)
                .withHeader("Content-Type", "text/plain")
                .withBody("hello world")));

        try {
            EnqueueDecorator.enqueue(service.greeting());
        } catch (Throwable ignored) {
        }

        final CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfFailedCalls())
            .describedAs("Failed calls")
            .isEqualTo(1);

        // Circuit breaker should still be closed, not hit open threshold
        assertThat(circuitBreaker.getState())
            .isEqualTo(CircuitBreaker.State.CLOSED);

        try {
            EnqueueDecorator.enqueue(service.greeting());
        } catch (Throwable ignored) {
        }

        try {
            EnqueueDecorator.enqueue(service.greeting());
        } catch (Throwable ignored) {
        }

        assertThat(metrics.getNumberOfFailedCalls())
            .isEqualTo(3);
        // Circuit breaker should be OPEN, threshold met
        assertThat(circuitBreaker.getState())
            .isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    public void decorateUnsuccessfulCall() throws Exception {
        stubFor(get(urlPathEqualTo("/greeting"))
            .willReturn(aResponse()
                .withStatus(500)
                .withHeader("Content-Type", "text/plain")));

        final Response<String> response = service.greeting().execute();

        assertThat(response.code())
            .describedAs("Response code")
            .isEqualTo(500);

        final CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(1);
    }

    @Test
    public void decorateUnsuccessfulEnqueuedCall() throws Throwable {
        stubFor(get(urlPathEqualTo("/greeting"))
            .willReturn(aResponse()
                .withStatus(500)
                .withHeader("Content-Type", "text/plain")));

        final Response<String> response = EnqueueDecorator.enqueue(service.greeting());

        assertThat(response.code())
            .describedAs("Response code")
            .isEqualTo(500);

        final CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(1);
    }

    @Test
    public void shouldNotCallServiceOnEnqueueWhenOpen() throws Throwable {
        stubFor(get(urlPathEqualTo("/greeting"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/plain")
                .withBody("hello world")));

        circuitBreaker.transitionToOpenState();

        try {
            EnqueueDecorator.enqueue(service.greeting());
            fail("CallNotPermittedException was expected");
        } catch (CallNotPermittedException ignore) {

        }

        ensureAllRequestsAreExecuted(Duration.ofSeconds(1));
        verify(0, getRequestedFor(urlPathEqualTo("/greeting")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowOnBadService() {
        BadRetrofitService badService = new Retrofit.Builder()
            .addCallAdapterFactory(CircuitBreakerCallAdapter.of(circuitBreaker))
            .baseUrl(wireMockRule.baseUrl())
            .build()
            .create(BadRetrofitService.class);

        badService.greeting();
    }

    @Test
    public void shouldDelegateToOtherAdapter() {
        String body = "this is from rxjava";

        stubFor(get(urlPathEqualTo("/delegated"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/plain")
                .withBody(body)));

        RetrofitService service = new Retrofit.Builder()
            .addCallAdapterFactory(CircuitBreakerCallAdapter.of(circuitBreaker))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(ScalarsConverterFactory.create())
            .baseUrl(wireMockRule.baseUrl())
            .client(client)
            .build()
            .create(RetrofitService.class);

        String resultBody = service.delegated().blockingGet();
        assertThat(resultBody).isEqualTo(body);
        verify(1, getRequestedFor(urlPathEqualTo("/delegated")));

        final CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(1);
    }

    @Test
    public void shouldNotDelegateToOtherAdapterWhenAddedAfterwards() {
        String body = "this is from rxjava";

        stubFor(get(urlPathEqualTo("/delegated"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/plain")
                .withBody(body)));

        RetrofitService service = new Retrofit.Builder()
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addCallAdapterFactory(CircuitBreakerCallAdapter.of(circuitBreaker))
            .addConverterFactory(ScalarsConverterFactory.create())
            .baseUrl(wireMockRule.baseUrl())
            .client(client)
            .build()
            .create(RetrofitService.class);

        String resultBody = service.delegated().blockingGet();
        assertThat(resultBody).isEqualTo(body);
        verify(1, getRequestedFor(urlPathEqualTo("/delegated")));

        // No metrics should exist because circuit breaker wasn't used
        final CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(0);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(metrics.getNumberOfNotPermittedCalls()).isEqualTo(0);
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);
    }

    private void ensureAllRequestsAreExecuted(Duration timeout) throws InterruptedException {
        long end = System.nanoTime() + timeout.toNanos();
        Dispatcher dispatcher = client.dispatcher();
        while (System.nanoTime() < end) {
            if (dispatcher.queuedCallsCount() <= 0 && dispatcher.runningCallsCount() <= 0) {
                return;
            }
            Thread.sleep(10);
        }
        fail("Timeout exceeded while waiting for requests to be finished");
    }

    private void cancelAsync(Call<?> call, long delayMs) {
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(delayMs);
            } catch (Exception ignored) {
            }
            call.cancel();
        });
    }
}
