/*
 * Copyright 2020 Michael Pollind
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.micronaut.timelimiter

import io.github.resilience4j.timelimiter.TimeLimiterRegistry
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification



@MicronautTest
@Property(name = "resilience4j.timelimiter.enabled", value = "true")
@Property(name = "resilience4j.timelimiter.configs.default.timeoutDuration", value = "PT1S")
@Property(name = "resilience4j.timelimiter.configs.default.cancelRunningFuture", value = "true")
@Property(name = "resilience4j.timelimiter.instances.backendA.baseConfig", value = "default")
@Property(name = "resilience4j.timelimiter.instances.backendA.timeoutDuration", value = "PT2S")
@Property(name = "resilience4j.timelimiter.instances.backendA.cancelRunningFuture", value = "false")
class TimeLimiterRegistrySpec extends Specification{

    @Inject
    ApplicationContext applicationContext;

    void "default configuration"() {
        given:
        def registry = applicationContext.getBean(TimeLimiterRegistry)
        def defaultTimeLimiter = registry.timeLimiter("default")

        expect:
        defaultTimeLimiter != null
        defaultTimeLimiter.timeLimiterConfig.timeoutDuration.seconds == 1
        defaultTimeLimiter.timeLimiterConfig.shouldCancelRunningFuture()
        defaultTimeLimiter.getName() == "default"
    }

    void "backend-a configuration"() {
        given:
        def registry = applicationContext.getBean(TimeLimiterRegistry)
        def backATimeLimiter = registry.timeLimiter("backend-a")

        expect:
        backATimeLimiter != null
        backATimeLimiter.timeLimiterConfig.timeoutDuration.seconds == 2
        !backATimeLimiter.timeLimiterConfig.shouldCancelRunningFuture()
    }
}
