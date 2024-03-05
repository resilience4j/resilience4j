package io.github.resilience4j.feign;


import com.github.tomakehurst.wiremock.junit.WireMockRule;
import feign.Feign;
import feign.FeignException;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.feign.test.TestService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.mockito.Mockito.*;

/**
 * Tests the integration of the {@link Resilience4jFeign} with a bulkhead.
 */
public class Resilience4jFeignBulkheadTest {

    private static final String MOCK_URL = "http://localhost:8080/";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule();

    private TestService testService;
    private Bulkhead bulkhead;

    @Before
    public void setUp() {
        bulkhead = spy(Bulkhead.of("bulkheadTest", BulkheadConfig.ofDefaults()));
        final FeignDecorators decorators = FeignDecorators.builder()
                .withBulkhead(bulkhead)
                .build();
        testService = Feign.builder()
                .addCapability(Resilience4jFeign.capability(decorators))
                .target(TestService.class, MOCK_URL);

    }

    @Test
    public void testSuccessfulCall() {
        givenResponse(200);

        testService.greeting();

        verify(1, getRequestedFor(urlPathEqualTo("/greeting")));
    }

    @Test(expected = BulkheadFullException.class)
    public void testBulkheadFull() {
        givenResponse(200);

        when(bulkhead.tryAcquirePermission()).thenReturn(false);

        testService.greeting();

        verify(0, getRequestedFor(urlPathEqualTo("/greeting")));
    }

    @Test(expected = FeignException.class)
    public void testFailedCall() {
        givenResponse(400);

        when(bulkhead.tryAcquirePermission()).thenReturn(true);

        testService.greeting();
    }


    private void givenResponse(int responseCode) {
        stubFor(get(urlPathEqualTo("/greeting"))
                .willReturn(aResponse()
                        .withStatus(responseCode)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("hello world")));
    }
}
