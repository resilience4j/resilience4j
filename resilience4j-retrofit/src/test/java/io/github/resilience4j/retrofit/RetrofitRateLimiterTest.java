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
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.reactivex.Single;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.HttpException;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Tests the integration of the Retrofit HTTP client and {@link RateLimiter}
 */
public class RetrofitRateLimiterTest {

    private static final RateLimiterConfig config = RateLimiterConfig.custom()
        .timeoutDuration(Duration.ofMillis(50))
        .limitRefreshPeriod(Duration.ofMillis(5000))
        .limitForPeriod(1)
        .build();
    @Rule
    public WireMockRule wireMockRule = new WireMockRule();
    private RetrofitService service;
    private RateLimiter rateLimiter;
    private OkHttpClient client;

    @Before
    public void setUp() {
        final long TIMEOUT = 150; // ms
        this.client = new OkHttpClient.Builder()
            .connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
            .readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
            .writeTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
            .build();

        this.rateLimiter = RateLimiter.of("backendName", config);
        this.service = new Retrofit.Builder()
            .addCallAdapterFactory(RateLimiterCallAdapter.of(rateLimiter))
            .addConverterFactory(ScalarsConverterFactory.create())
            .client(client)
            .baseUrl(wireMockRule.baseUrl())
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

    @Test(expected = IOException.class)
    public void shouldNotCatchCallExceptionsInRateLimiter() throws Exception {
        stubFor(get(urlPathEqualTo("/greeting"))
            .willReturn(aResponse()
                .withStatus(200)
                .withFixedDelay(400)));

        service.greeting().execute();
    }

    @Test(expected = IOException.class)
    public void shouldNotCatchEnqueuedCallExceptionsInRateLimiter() throws Throwable {
        stubFor(get(urlPathEqualTo("/greeting"))
            .willReturn(aResponse()
                .withStatus(200)
                .withFixedDelay(400)));

        EnqueueDecorator.enqueue(service.greeting());
    }

    @Test
    public void decorateRateLimitedCall() throws Exception {
        stubFor(get(urlPathEqualTo("/greeting"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/plain")
                .withBody("hello world")));

        final Response<String> execute = service.greeting().execute();
        assertThat(execute.isSuccessful())
            .describedAs("Response successful")
            .isTrue();

        final Response<String> rateLimitedResponse = service.greeting().execute();
        assertThat(rateLimitedResponse.isSuccessful())
            .describedAs("Response successful")
            .isFalse();
        assertThat(rateLimitedResponse.code())
            .describedAs("HTTP Error Code")
            .isEqualTo(429);
    }

    @Test
    public void decorateRateLimitedEnqueuedCall() throws Throwable {
        stubFor(get(urlPathEqualTo("/greeting"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/plain")
                .withBody("hello world")));

        final Response<String> execute = EnqueueDecorator.enqueue(service.greeting());
        assertThat(execute.isSuccessful())
            .describedAs("Response successful")
            .isTrue();

        final Response<String> rateLimitedResponse = EnqueueDecorator.enqueue(service.greeting());
        assertThat(rateLimitedResponse.isSuccessful())
            .describedAs("Response successful")
            .isFalse();
        assertThat(rateLimitedResponse.code())
            .describedAs("HTTP Error Code")
            .isEqualTo(429);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowOnBadService() {
        BadRetrofitService badService = new Retrofit.Builder()
            .addCallAdapterFactory(RateLimiterCallAdapter.of(rateLimiter))
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
            .addCallAdapterFactory(RateLimiterCallAdapter.of(RateLimiter.of(
                "backendName",
                RateLimiterConfig.custom()
                    .timeoutDuration(Duration.ofMillis(50))
                    .limitForPeriod(1)
                    .limitRefreshPeriod(Duration.ofDays(1))
                    .build()
            )))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(ScalarsConverterFactory.create())
            .client(client)
            .baseUrl(wireMockRule.baseUrl())
            .build()
            .create(RetrofitService.class);

        Single<String> success = service.delegated();
        Single<String> failure = service.delegated();

        String resultBody = success.blockingGet();
        try {
            failure.blockingGet();
            fail("Expected HttpException to be thrown");
        } catch (HttpException httpe) {
            assertThat(httpe.code()).isEqualTo(429);
        }

        assertThat(resultBody).isEqualTo(body);
        verify(1, getRequestedFor(urlPathEqualTo("/delegated")));
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
            .addCallAdapterFactory(RateLimiterCallAdapter.of(RateLimiter.of(
                "backendName",
                RateLimiterConfig.custom()
                    .timeoutDuration(Duration.ofMillis(50))
                    .limitForPeriod(1)
                    .limitRefreshPeriod(Duration.ofDays(1))
                    .build()
            )))
            .addConverterFactory(ScalarsConverterFactory.create())
            .client(client)
            .baseUrl(wireMockRule.baseUrl())
            .build()
            .create(RetrofitService.class);

        Single<String> success = service.delegated();
        Single<String> failure = service.delegated();

        String resultBody = success.blockingGet();
        failure.blockingGet();

        assertThat(resultBody).isEqualTo(body);
        verify(2, getRequestedFor(urlPathEqualTo("/delegated")));
    }
}
