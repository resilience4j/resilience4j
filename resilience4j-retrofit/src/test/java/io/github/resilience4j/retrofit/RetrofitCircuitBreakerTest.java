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
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the integration of the Retrofit HTTP client and {@link CircuitBreaker}
 */
public class RetrofitCircuitBreakerTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule();

    private static final CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
            .ringBufferSizeInClosedState(3)
            .waitDurationInOpenState(Duration.ofMillis(1000))
            .build();

    private CircuitBreaker circuitBreaker = CircuitBreaker.of("test", circuitBreakerConfig);

    private RetrofitService service;

    @Before
    public void setUp() {
        this.circuitBreaker = CircuitBreaker.of("test", circuitBreakerConfig);

        final long TIMEOUT = 300; // ms
        final OkHttpClient client = new OkHttpClient.Builder()
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

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowOnBadService() {
        BadRetrofitService badService = new Retrofit.Builder()
                .addCallAdapterFactory(CircuitBreakerCallAdapter.of(circuitBreaker))
                .baseUrl("http://localhost:8080/")
                .build()
                .create(BadRetrofitService.class);

        badService.greeting();
    }
}