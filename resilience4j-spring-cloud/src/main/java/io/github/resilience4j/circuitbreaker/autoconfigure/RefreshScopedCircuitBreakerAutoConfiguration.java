package io.github.resilience4j.circuitbreaker.autoconfigure;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerConfigurationProperties;
import io.github.resilience4j.circuitbreaker.monitoring.health.CircuitBreakerHealthIndicator;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
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

    public RefreshScopedCircuitBreakerAutoConfiguration(ConfigurableBeanFactory beanFactory,
                                                        CircuitBreakerConfigurationProperties circuitBreakerProperties) {
        super(beanFactory, circuitBreakerProperties);
    }

    @Override
    protected void createHealthIndicatorForCircuitBreaker(CircuitBreaker circuitBreaker, CircuitBreakerConfigurationProperties circuitBreakerProperties) {
        boolean registerHealthIndicator = circuitBreakerProperties.findCircuitBreakerProperties(circuitBreaker.getName())
                .map(io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties::getRegisterHealthIndicator)
                .orElse(true);

        String circuitBreakerHealthIndicatorBeanName = circuitBreaker.getName() + "CircuitBreakerHealthIndicator";

        if (beanFactory instanceof DefaultListableBeanFactory) {
            Object previousSingleton = beanFactory.getSingleton(circuitBreakerHealthIndicatorBeanName);

            if (previousSingleton != null) {
                ((DefaultListableBeanFactory) beanFactory).destroySingleton(circuitBreakerHealthIndicatorBeanName);
            }
        }

        if (registerHealthIndicator) {
            CircuitBreakerHealthIndicator healthIndicator = new CircuitBreakerHealthIndicator(circuitBreaker);
            beanFactory.registerSingleton(circuitBreakerHealthIndicatorBeanName, healthIndicator);
        }
    }

}
