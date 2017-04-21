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
package io.github.resilience4j.ratpack

import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import ratpack.exec.Blocking
import ratpack.test.exec.ExecHarness
import spock.lang.Ignore
import spock.lang.Specification

import java.time.Duration

class RatelimiterTransformerSpec extends Specification {

  @Ignore
  def "can ratelimit promise then throw exception when limit is exceeded"() {
    given:
    RateLimiter rateLimiter = buildRatelimiter()
    RateLimiterTransformer<Integer> transformer = RateLimiterTransformer.of(rateLimiter)
    Set<Integer> values = [].toSet()
    Set<Integer> expected = (0..9).toSet()

    when:
    for (int i = 0 ; i <= 10; i++) {
      def r =  ExecHarness.yieldSingle {
        Blocking.<Integer> get {
          i
        }.transform(transformer)
      }
      if (r.success) values << r.value
    }

    then:
    values == expected
  }

  @Ignore
  def "can ratelimit promise then throw exception when limit is exceeded then call recover"() {
    given:
    String failure = "failure"
    RateLimiter rateLimiter = buildRatelimiter()
    RateLimiterTransformer<Integer> transformer = RateLimiterTransformer.of(rateLimiter).recover { t -> failure }
    Set<Integer> values = [].toSet()
    Set<Integer> expected = (0..9).toSet()

    when:
    for (int i = 0 ; i <= 10; i++) {
      def r =  ExecHarness.yieldSingle {
        Blocking.<Integer> get {
          i
        }.transform(transformer)
      }
      if (r.success) values << r.value
    }

    then:
    values == expected << failure
  }

  // 10 events / 10 s
  def buildRatelimiter() {
    RateLimiterConfig config = RateLimiterConfig.custom()
      .limitRefreshPeriod(Duration.ofSeconds(10))
      .limitForPeriod(10)
      .timeoutDuration(Duration.ofMillis(100))
      .build()
    RateLimiter.of("test", config)
  }
}
