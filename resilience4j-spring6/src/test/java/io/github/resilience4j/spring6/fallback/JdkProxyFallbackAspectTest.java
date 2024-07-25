package io.github.resilience4j.spring6.fallback;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.spring6.CircuitBreakerDummyService;
import io.github.resilience4j.spring6.TestApplication;
import io.github.resilience4j.spring6.TestDummyService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(
        classes = {TestApplication.class, JdkProxyFallbackAspectTest.TestConfig.class},
        properties = {"spring.aop.proxy-target-class=false"}
)
public class JdkProxyFallbackAspectTest {

    @Autowired
    @Qualifier("fallbackTestDummyService")
    TestDummyService testDummyService;

    @Test
    public void testFallbackAspect() {
        assertThat(testDummyService.sync()).isEqualTo("recovery");
    }

    public static class FallbackTestDummyService extends CircuitBreakerDummyService {

        @Override
        @CircuitBreaker(name = BACKEND, fallbackMethod = "fallback")
        public String sync() {
            return syncError();
        }

        public String fallback(RuntimeException throwable) {
            return "recovery";
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class TestConfig {

        @Bean
        public TestDummyService fallbackTestDummyService() {
            return new FallbackTestDummyService();
        }
    }
}
