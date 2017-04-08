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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Tests the integration of the Retrofit HTTP client and {@link RateLimiter}
 */
public class RetrofitRateLimiterTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule();

    private RetrofitService service;
    private final RateLimiterConfig config = RateLimiterConfig.custom()
            .timeoutDuration(Duration.ofMillis(100))
            .limitRefreshPeriod(Duration.ofSeconds(1))
            .limitForPeriod(1)
            .build();
    private final RateLimiter rateLimiter = RateLimiter.of("backendName", config);

    @Before
    public void setUp() {
        this.service = new Retrofit.Builder()
                .addCallAdapterFactory(RateLimiterCallAdapter.of(rateLimiter))
                .addConverterFactory(ScalarsConverterFactory.create())
                .baseUrl("http://localhost:8080/")
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


    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowOnBadService() {
        BadRetrofitService badService = new Retrofit.Builder()
                .addCallAdapterFactory(RateLimiterCallAdapter.of(rateLimiter))
                .baseUrl("http://localhost:8080/")
                .build()
                .create(BadRetrofitService.class);

        badService.greeting();
    }
}