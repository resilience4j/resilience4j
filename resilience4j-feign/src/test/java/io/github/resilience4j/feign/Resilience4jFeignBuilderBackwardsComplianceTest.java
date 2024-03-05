package io.github.resilience4j.feign;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import feign.Feign;
import io.github.resilience4j.feign.test.TestService;
import org.junit.ClassRule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class Resilience4jFeignBuilderBackwardsComplianceTest {
    @ClassRule
    public static final WireMockClassRule WIRE_MOCK_RULE = new WireMockClassRule(8080);

    @Test
    public void fallbackIsWorkingInBothConfigurationMechanisms() {
        FeignDecorators decorators = FeignDecorators.builder()
                .withFallback((TestService) () -> "fallback").build();

        TestService testServiceA = Feign.builder()
                .addCapability(Resilience4jFeign.capability(decorators))
                .target(TestService.class, "http://localhost:8080/");

        TestService testServiceB = Resilience4jFeign.builder(decorators)
                .target(TestService.class, "http://localhost:8080/");

        String resultA = testServiceA.greeting();
        String resultB = testServiceB.greeting();

        assertThat(resultA).isEqualTo("fallback");
        assertThat(resultA).isEqualTo(resultB);
    }
}
