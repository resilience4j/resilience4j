package io.github.resilience4j.micronaut.retry

import io.github.resilience4j.common.retry.configuration.RetryConfigurationProperties
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.http.annotation.Controller
import io.micronaut.test.annotation.MicronautTest
import spock.lang.Specification

import javax.inject.Inject

@MicronautTest
@Property(name = "resilience4j.retry.enabled", value = "true")
class RetySpec extends Specification{
   @Inject ApplicationContext applicationContext

    void "default configuration"() {
        given:
        def config = applicationContext.getBean(RetryConfigurationProperties)

        expect:
        def defaultConfig = config.configs['default']
//        defaultConfig.ignoreExceptions.contains()
    }


    @Controller("/retry")
    static class RetryController {

    }
}
