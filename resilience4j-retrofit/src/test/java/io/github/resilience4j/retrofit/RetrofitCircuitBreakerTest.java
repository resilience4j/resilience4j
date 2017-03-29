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
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;

/**
 * Tests the integration of the Retrofit HTTP client and {@link CircuitBreaker}
 */
public class RetrofitCircuitBreakerTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule();

    private RetrofitService service;
    private final CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
            .ringBufferSizeInClosedState(3)
            .waitDurationInOpenState(Duration.ofMillis(1000))
            .build();
    private final CircuitBreaker circuitBreaker = CircuitBreaker.of("test", circuitBreakerConfig);

    @Before
    public void setUp() {
        final long TIMEOUT = 300; // ms
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                .writeTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                .build();

        this.service = new Retrofit.Builder()
                .addCallAdapterFactory(CircuitBreakerCallAdapter.of(circuitBreaker))
                .addConverterFactory(ScalarsConverterFactory.create())
                .baseUrl("http://localhost:8080/")
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
        assertEquals(1, metrics.getNumberOfFailedCalls());

        // Circuit breaker should still be closed, not hit open threshold
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());

        try {
            service.greeting().execute();
        } catch (Throwable ignored) {
        }

        try {
            service.greeting().execute();
        } catch (Throwable ignored) {
        }

        assertEquals(3, metrics.getNumberOfFailedCalls());
        // Circuit breaker should be OPEN, threshold met
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
    }

    @Test
    public void passThroughCallsToDecoratedObject() {
        final Call<String> call = mock(StringCall.class);
        final Call<String> decorated = RetrofitCircuitBreaker.decorateCall(circuitBreaker, call, Response::isSuccessful);

        decorated.cancel();
        Mockito.verify(call).cancel();

        decorated.enqueue(null);
        Mockito.verify(call).enqueue(any());

        decorated.isExecuted();
        Mockito.verify(call).isExecuted();

        decorated.isCanceled();
        Mockito.verify(call).isCanceled();

        decorated.clone();
        Mockito.verify(call).clone();

        decorated.request();
        Mockito.verify(call).request();
    }

    private interface StringCall extends Call<String> {
    }

}