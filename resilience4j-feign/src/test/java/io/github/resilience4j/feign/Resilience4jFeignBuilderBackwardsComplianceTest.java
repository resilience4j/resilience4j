/*
 *
 * Copyright 2026
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
 *
 */
package io.github.resilience4j.feign;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import feign.Feign;
import io.github.resilience4j.feign.test.TestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@WireMockTest
class Resilience4jFeignBuilderBackwardsComplianceTest {

    private String baseUrl;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
        baseUrl = wmRuntimeInfo.getHttpBaseUrl() + "/";
    }

    @Test
    void fallbackIsWorkingInBothConfigurationMechanisms() {
        FeignDecorators decorators = FeignDecorators.builder()
                .withFallback((TestService) () -> "fallback").build();

        TestService testServiceA = Feign.builder()
                .addCapability(Resilience4jFeign.capability(decorators))
                .target(TestService.class, baseUrl);

        TestService testServiceB = Resilience4jFeign.builder(decorators)
                .target(TestService.class, baseUrl);

        String resultA = testServiceA.greeting();
        String resultB = testServiceB.greeting();

        assertThat(resultA)
                .isEqualTo("fallback")
                .isEqualTo(resultB);
    }
}
