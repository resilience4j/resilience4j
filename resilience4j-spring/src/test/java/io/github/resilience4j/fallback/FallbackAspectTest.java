package io.github.resilience4j.fallback;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.CircuitBreakerDummyService;
import io.github.resilience4j.TestApplication;
import io.github.resilience4j.TestDummyService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {TestApplication.class, FallbackAspectTest.TestConfig.class})
public class FallbackAspectTest {

    @Autowired
    @Qualifier("fallbackTestDummyService")
    TestDummyService testDummyService;


    @Test
    public void testFallbackAspect() {
        assertThat(testDummyService.sync()).isEqualTo("aspect");
    }

    @Component
    public static class FallbackTestDummyService extends CircuitBreakerDummyService {
        @Override
        @CircuitBreaker(name = BACKEND, fallbackMethod = "fallback")
        public String sync() {
            return syncError();
        }

        @TestAround
        public String fallback(RuntimeException throwable) {
            return "recovery";
        }
    }

    @Configuration
    static class TestConfig {

        @Bean
        public FallbackTestDummyService fallbackTestDummyService() {
            return new FallbackTestDummyService();
        }

        @Bean
        public TestAspect testAspect() {
            return new TestAspect();
        }
    }

    @Aspect
    static class TestAspect {

        @Around("@annotation(FallbackAspectTest.TestAround)")
        public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
            joinPoint.proceed();
            return "aspect";
        }
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface TestAround {
    }
}
