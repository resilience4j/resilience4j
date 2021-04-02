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
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the integration of the Retrofit HTTP client and {@link Retry}
 */
public class RetrofitRetryTest {

    private static final RetryConfig retryConfig = RetryConfig.<Response<String>>custom().maxAttempts(3).retryOnResult(response -> response.code() == 504)
        .intervalFunction(IntervalFunction.ofExponentialBackoff())
        .retryExceptions(IOException.class)
        .build();
    @Rule
    public WireMockRule wireMockRule = new WireMockRule();
    private Retry retry;
    private OkHttpClient client;
    private RetrofitService service;

    @Before
    public void setUp() {
        this.retry = Retry.of("test", retryConfig);

        final long TIMEOUT = 150; // ms
        this.client = new OkHttpClient.Builder()
            .connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
            .readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
            .writeTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
            .build();

        this.service = new Retrofit.Builder()
            .addCallAdapterFactory(RetryCallAdapter.of(retry))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .baseUrl(wireMockRule.baseUrl())
            .client(client)
            .build()
            .create(RetrofitService.class);
    }

    @Test
    public void decorateSuccessfulCall() throws Exception {
        stubFor(get(urlPathEqualTo("/greetings"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/plain")
                .withBody("hello world")));

        service.greetings().execute();

        verify(1, getRequestedFor(urlPathEqualTo("/greetings")));
    }

    @Test
    public void decorateSuccessfulEnqueuedCall() throws Throwable {
        stubFor(get(urlPathEqualTo("/greetings"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/plain")
                .withBody("hello world")));

        EnqueueDecorator.enqueue(service.greetings());

        verify(1, getRequestedFor(urlPathEqualTo("/greetings")));
    }

    //Test Retry for IO Exception
    @Test
    public void decorateTimingOutCall() {
        stubFor(get(urlPathEqualTo("/greetings"))
            .willReturn(aResponse()
                .withFixedDelay(500)
                .withStatus(200)
                .withHeader("Content-Type", "text/plain")
                .withBody("hello world")));

        try {
            service.greetings().execute();
        } catch (Throwable ignored) {
        }

        final Retry.Metrics metrics = retry.getMetrics();
        verify(3, getRequestedFor(urlPathEqualTo("/greetings")));
        assertThat(metrics.getNumberOfSuccessfulCallsWithRetryAttempt())
            .isEqualTo(0);
    }

    @Test
    public void decorateCancelledCall() {
        stubFor(get(urlPathEqualTo("/greetings"))
            .willReturn(aResponse()
                .withFixedDelay(500)
                .withStatus(200)
                .withHeader("Content-Type", "text/plain")
                .withBody("hello world")));

        try {
            Call<ResponseBody> call = service.greetings();
            cancelAsync(call, 100);
            call.execute();
        } catch (Throwable ignored) {
        }

        verify(3, getRequestedFor(urlPathEqualTo("/greetings")));
        final Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfSuccessfulCallsWithRetryAttempt())
            .isEqualTo(0);
    }

    @Test
    public void decorateCancelledEnqueuedCall() {
        stubFor(get(urlPathEqualTo("/greetings"))
            .willReturn(aResponse()
                .withFixedDelay(500)
                .withStatus(200)
                .withHeader("Content-Type", "text/plain")
                .withBody("hello world")));

        Call<ResponseBody> call = service.greetings();
        cancelAsync(call, 100);
        EnqueueDecorator.performCatchingEnqueue(call);

        final Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt())
            .isEqualTo(0);

        verify(3, getRequestedFor(urlPathEqualTo("/greetings")));
    }

    @Test
    public void decorateTimingOutEnqueuedCall() {
        stubFor(get(urlPathEqualTo("/greetings"))
            .willReturn(aResponse()
                .withFixedDelay(500)
                .withStatus(200)
                .withHeader("Content-Type", "text/plain")
                .withBody("hello world")));

        try {
            EnqueueDecorator.enqueue(service.greetings());
        } catch (Throwable ignored) {
        }

        final Retry.Metrics metrics = retry.getMetrics();
        verify(3, getRequestedFor(urlPathEqualTo("/greetings")));
        assertThat(metrics.getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(0);
    }

    @Test
    public void decorateUnsuccessfulCall() throws Exception {
        stubFor(get(urlPathEqualTo("/greetings"))
            .willReturn(aResponse()
                .withStatus(504)
                .withHeader("Content-Type", "text/plain")));

        final Response<ResponseBody> response = service.greetings().execute();

        assertThat(response.code())
            .describedAs("Response code")
            .isEqualTo(504);

        verify(3, getRequestedFor(urlPathEqualTo("/greetings")));
    }

    @Test
    public void decorateUnsuccessfulEnqueuedCall() throws Throwable {

        stubFor(get(urlPathEqualTo("/greetings"))
            .willReturn(aResponse()
                .withStatus(504)
                .withHeader("Content-Type", "text/plain")));

        final Response<ResponseBody> response = EnqueueDecorator.enqueue(service.greetings());

        assertThat(response.code())
            .describedAs("Response code")
            .isEqualTo(504);

        verify(3, getRequestedFor(urlPathEqualTo("/greetings")));
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
