/*
 *
 * Copyright 2021 Ipuvi Mishra
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
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
    public void shouldNotRetrySuccessfulCall() throws Exception {
        stubFor(get(urlPathEqualTo("/greetingsResponse"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/plain")
                .withBody("hello world")));

        service.greetingsResponse().execute();

        verify(1, getRequestedFor(urlPathEqualTo("/greetingsResponse")));
    }

    @Test
    public void shouldNotRetrySuccessfulEnqueuedCall() throws Throwable {
        stubFor(get(urlPathEqualTo("/greetingsResponse"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/plain")
                .withBody("hello world")));

        EnqueueDecorator.enqueue(service.greetingsResponse());

        verify(1, getRequestedFor(urlPathEqualTo("/greetingsResponse")));
    }


    @Test(expected = IOException.class)
    public void shouldRetryOnIOExceptionTimingOutCall() throws IOException {
        stubFor(get(urlPathEqualTo("/greetingsResponse"))
            .willReturn(aResponse()
                .withFixedDelay(500)
                .withStatus(200)
                .withHeader("Content-Type", "text/plain")
                .withBody("hello world")));

        try {
            service.greetingsResponse().execute();
        } catch (Exception e) {
            throw new IOException();
        }

        final Retry.Metrics metrics = retry.getMetrics();
        verify(3, getRequestedFor(urlPathEqualTo("/greetingsResponse")));
        assertThat(metrics.getNumberOfSuccessfulCallsWithRetryAttempt())
            .isEqualTo(0);
    }

    @Test(expected = IOException.class)
    public void shouldRetryOnIOExceptionForCancelledCall() throws IOException {
        stubFor(get(urlPathEqualTo("/greetingsResponse"))
            .willReturn(aResponse()
                .withFixedDelay(500)
                .withStatus(200)
                .withHeader("Content-Type", "text/plain")
                .withBody("hello world")));

        try {
            Call<ResponseBody> call = service.greetingsResponse();
            cancelAsync(call, 100);
            call.execute();
        } catch (Exception e) {
            throw new IOException();
        }

        verify(3, getRequestedFor(urlPathEqualTo("/greetingsResponse")));
        final Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfSuccessfulCallsWithRetryAttempt())
            .isEqualTo(0);
    }

    @Test(expected = IOException.class)
    public void shouldRetryOnIOExceptionForCancelledEnqueuedCall() throws IOException {
        stubFor(get(urlPathEqualTo("/greetingsResponse"))
            .willReturn(aResponse()
                .withFixedDelay(500)
                .withStatus(200)
                .withHeader("Content-Type", "text/plain")
                .withBody("hello world")));

        Call<ResponseBody> call = service.greetingsResponse();
        cancelAsync(call, 100);
        try {
            EnqueueDecorator.enqueue(call);
        } catch (Throwable e) {
            throw new IOException();
        }

        final Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt())
            .isEqualTo(0);

        verify(3, getRequestedFor(urlPathEqualTo("/greetingsResponse")));
    }

    @Test(expected = IOException.class)
    public void shouldRetryOnIOExceptionTimingOutEnqueuedCall() throws IOException {
        stubFor(get(urlPathEqualTo("/greetingsResponse"))
            .willReturn(aResponse()
                .withFixedDelay(500)
                .withStatus(200)
                .withHeader("Content-Type", "text/plain")
                .withBody("hello world")));

        try {
            EnqueueDecorator.enqueue(service.greetingsResponse());
        } catch (Throwable e) {
            throw new IOException();
        }

        final Retry.Metrics metrics = retry.getMetrics();
        verify(3, getRequestedFor(urlPathEqualTo("/greetingsResponse")));
        assertThat(metrics.getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(0);
    }

    @Test
    public void shouldRetryOn504ForUnsuccessfulCall() throws Exception {
        stubFor(get(urlPathEqualTo("/greetingsResponse"))
            .willReturn(aResponse()
                .withStatus(504)
                .withHeader("Content-Type", "text/plain")));

        final Response<ResponseBody> response = service.greetingsResponse().execute();

        assertThat(response.code())
            .describedAs("Response code")
            .isEqualTo(504);

        verify(3, getRequestedFor(urlPathEqualTo("/greetingsResponse")));
    }

    @Test
    public void shouldRetryOn504ForUnsuccessfulEnqueuedCall() throws Throwable {

        stubFor(get(urlPathEqualTo("/greetingsResponse"))
            .willReturn(aResponse()
                .withStatus(504)
                .withHeader("Content-Type", "text/plain")));

        final Response<ResponseBody> response = EnqueueDecorator.enqueue(service.greetingsResponse());

        assertThat(response.code())
            .describedAs("Response code")
            .isEqualTo(504);

        verify(3, getRequestedFor(urlPathEqualTo("/greetingsResponse")));
    }

    private void cancelAsync(Call<?> call, long delayMs) {
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(delayMs);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            call.cancel();
        });
    }
}
