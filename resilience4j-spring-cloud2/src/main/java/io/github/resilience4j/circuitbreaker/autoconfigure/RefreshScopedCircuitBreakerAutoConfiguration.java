package io.github.resilience4j.circuitbreaker.autoconfigure;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerConfigurationProperties;
import io.github.resilience4j.circuitbreaker.monitoring.health.CircuitBreakerHealthIndicator;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.actuate.health.HealthIndicatorRegistry;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.context.scope.refresh.RefreshScope;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@ConditionalOnClass({CircuitBreaker.class, RefreshScope.class})
@AutoConfigureAfter(RefreshAutoConfiguration.class)
@AutoConfigureBefore(CircuitBreakerAutoConfiguration.class)
public class RefreshScopedCircuitBreakerAutoConfiguration extends AbstractRefreshScopedCircuitBreakerConfiguration implements ApplicationContextAware {

    private ApplicationContext applicationContext;
    private HealthIndicatorRegistry healthIndicatorRegistry;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext){
        this.applicationContext = applicationContext;
    }

    public RefreshScopedCircuitBreakerAutoConfiguration(ConfigurableBeanFactory beanFactory,
                                                        CircuitBreakerConfigurationProperties circuitBreakerProperties) {
        super(beanFactory, circuitBreakerProperties);
    }

    @Override
    protected void createHealthIndicatorForCircuitBreaker(CircuitBreaker circuitBreaker, CircuitBreakerConfigurationProperties circuitBreakerProperties) {
        boolean registerHealthIndicator = circuitBreakerProperties.findCircuitBreakerProperties(circuitBreaker.getName())
                .map(io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties::getRegisterHealthIndicator)
                .orElse(true);

        String circuitBreakerName = circuitBreaker.getName() + "CircuitBreaker";
        String circuitBreakerHealthIndicatorBeanName = circuitBreakerName + "HealthIndicator";

        if (beanFactory instanceof DefaultListableBeanFactory) {
            Object previousSingleton = beanFactory.getSingleton(circuitBreakerHealthIndicatorBeanName);

            if (previousSingleton != null) {
                ((DefaultListableBeanFactory) beanFactory).destroySingleton(circuitBreakerHealthIndicatorBeanName);
            }
        }

        if (registerHealthIndicator) {

            CircuitBreakerHealthIndicator healthIndicator = new CircuitBreakerHealthIndicator(circuitBreaker);
            beanFactory.registerSingleton(
                    circuitBreakerHealthIndicatorBeanName,
                    healthIndicator
            );
            // To support health indicators created after the health registry was created, look up to see if it's in
            // the application context. If it is, save it off so we don't need to search for it again, then register
            // the new health indicator with the registry.
            if (applicationContext != null && healthIndicatorRegistry == null) {
                Map<String, HealthIndicatorRegistry> healthRegistryBeans = applicationContext.getBeansOfType(HealthIndicatorRegistry.class);
                if (healthRegistryBeans.size() > 0) {
                    healthIndicatorRegistry = healthRegistryBeans.values().iterator().next();
                }
            }

            if (healthIndicatorRegistry != null && healthIndicatorRegistry.get(circuitBreakerName) == null) {
                healthIndicatorRegistry.register(circuitBreakerName, healthIndicator);
            }
        }
    }
}
