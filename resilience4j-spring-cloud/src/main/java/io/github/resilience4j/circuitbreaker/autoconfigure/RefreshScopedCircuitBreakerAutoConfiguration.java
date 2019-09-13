package io.github.resilience4j.circuitbreaker.autoconfigure;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerConfigurationProperties;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.context.scope.refresh.RefreshScope;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass({CircuitBreaker.class, RefreshScope.class})
@AutoConfigureAfter(RefreshAutoConfiguration.class)
@AutoConfigureBefore(CircuitBreakerAutoConfiguration.class)
public class RefreshScopedCircuitBreakerAutoConfiguration extends AbstractRefreshScopedCircuitBreakerConfiguration {

    public RefreshScopedCircuitBreakerAutoConfiguration(CircuitBreakerConfigurationProperties circuitBreakerProperties) {
        super(circuitBreakerProperties);
    }

}
