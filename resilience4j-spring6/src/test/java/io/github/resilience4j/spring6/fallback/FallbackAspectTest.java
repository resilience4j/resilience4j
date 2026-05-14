package io.github.resilience4j.spring6.fallback;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.spring6.CircuitBreakerDummyService;
import io.github.resilience4j.spring6.TestApplication;
import io.github.resilience4j.spring6.TestDummyService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {TestApplication.class, FallbackAspectTest.TestConfig.class})
class FallbackAspectTest {

    @Autowired
    @Qualifier("fallbackTestDummyService")
    TestDummyService testDummyService;

    @Autowired
    @Qualifier("fallbackDependencyTestDummyService")
    TestDummyService testDependencyDummyService;

    @Test
    void testFallbackAspect() {
        AssertionsForClassTypes.assertThat(testDummyService.sync()).isEqualTo("aspect");
        AssertionsForClassTypes.assertThat(testDependencyDummyService.sync()).isEqualTo("dependency");
    }

    static class FallbackTestDummyService extends CircuitBreakerDummyService {

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

    static class FallbackDependencyTestDummyService extends CircuitBreakerDummyService {
        private final DependencyTestDummyService dependencyTestDummyService;

        public FallbackDependencyTestDummyService(DependencyTestDummyService dependencyTestDummyService) {
            this.dependencyTestDummyService = dependencyTestDummyService;
        }

        @Override
        @CircuitBreaker(name = BACKEND, fallbackMethod = "fallback")
        public String sync() {
            return syncError();
        }

        @TestAround
        private String fallback(RuntimeException throwable) {
            return dependencyTestDummyService.test();
        }
    }

    static class DependencyTestDummyService {
        public String test() {
            return "dependency";
        }
    }

    @Configuration
    static class TestConfig {

        @Bean
        public DependencyTestDummyService testService() {
            return new DependencyTestDummyService();
        }

        @Bean
        public FallbackTestDummyService fallbackTestDummyService() {
            return new FallbackTestDummyService();
        }

        @Bean
        public FallbackDependencyTestDummyService fallbackDependencyTestDummyService(DependencyTestDummyService dependencyTestDummyService) {
            return new FallbackDependencyTestDummyService(dependencyTestDummyService);
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
