/*
 * Copyright 2017 Dan Maas
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
package io.github.resilience4j.ratpack.timelimiter

import io.github.resilience4j.timelimiter.TimeLimiter
import io.github.resilience4j.timelimiter.TimeLimiterConfig
import ratpack.exec.ExecResult
import ratpack.exec.Promise
import ratpack.test.exec.ExecHarness
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration
import java.util.concurrent.TimeoutException

@Unroll
class TimeLimiterTransformerSpec extends Specification {

    def "can timelimit promise then throw exception when timeout is exceeded - #status"() {
        given:
        String value = "test"
        TimeLimiter timeLimiter = buildTimelimiter(Duration.ofMillis(500))
        TimeLimiterTransformer<String> transformer = TimeLimiterTransformer.of(timeLimiter)

        when:
        ExecResult<String> r = ExecHarness.yieldSingle {
            Promise.<String>async { down ->
                try {
                    Thread.start {
                        sleep(delay.toMillis())
                        down.success(value)
                    }
                } catch(Throwable t) {
                    down.error(t)
                }
            }.transform(transformer)
        }

        then:
        if (status == "ok") {
            assert r.value == value
        } else if (status == "timeout") {
            assert r.error
            assert r.throwable instanceof TimeoutException
        }

        where:
        status    | delay
        "ok"      | Duration.ofMillis(400)
        "timeout" | Duration.ofMillis(600)
    }

    // 10 events / 1 minute
    def buildTimelimiter(Duration duration) {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
            .timeoutDuration(duration)
            .build()
        TimeLimiter.of("test", config)
    }
}
