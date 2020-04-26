package io.github.resilience4j.micronaut.retry

import io.github.resilience4j.common.retry.configuration.RetryConfigurationProperties
import io.micronaut.context.ApplicationContext
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

class RetryConfigurationSpec extends Specification{
    @Shared
    @AutoCleanup
    ApplicationContext applicationContext = ApplicationContext.run(
        "resilience4j.retry.configs.default.waitDuration": 100,
        "resilience4j.retry.configs.default.limitRefreshPeriod": 100,
        "resilience4j.retry.configs.default.retryExceptionPredicate": ["java.io.IOException"],

        "resilience4j.retry.enabled" : true,
    )
    void "default configuration"() {
        given:
        def config = applicationContext.getBean(RetryConfigurationProperties)

        expect:
        def defaultConfig = config.configs['default']
        defaultConfig.limitRefreshPeriod.seconds == 100
    }


}
