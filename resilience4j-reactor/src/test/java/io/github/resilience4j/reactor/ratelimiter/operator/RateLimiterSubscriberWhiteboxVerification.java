/*
 * Copyright 2018 Julien Hoarau
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
package io.github.resilience4j.reactor.ratelimiter.operator;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.SubscriberWhiteboxVerification;
import org.reactivestreams.tck.TestEnvironment;
import reactor.core.publisher.MonoProcessor;

public class RateLimiterSubscriberWhiteboxVerification extends
    SubscriberWhiteboxVerification<Integer> {

    public RateLimiterSubscriberWhiteboxVerification() {
        super(new TestEnvironment());
    }

    @Override
    public Subscriber<Integer> createSubscriber(WhiteboxSubscriberProbe<Integer> probe) {
        return new RateLimiterSubscriber<Integer>(MonoProcessor.create()) {
            @Override
            public void hookOnSubscribe(Subscription subscription) {
                super.hookOnSubscribe(subscription);

                // register a successful Subscription, and create a Puppet,
                // for the WhiteboxVerification to be able to drive its tests:
                probe.registerOnSubscribe(new SubscriberPuppet() {

                    @Override
                    public void triggerRequest(long elements) {
                        subscription.request(elements);
                    }

                    @Override
                    public void signalCancel() {
                        subscription.cancel();
                    }
                });
            }

            @Override
            public void hookOnNext(Integer integer) {
                super.hookOnNext(integer);
                probe.registerOnNext(integer);
            }

            @Override
            public void hookOnError(Throwable t) {
                super.hookOnError(t);
                probe.registerOnError(t);
            }

            @Override
            public void hookOnComplete() {
                super.hookOnComplete();
                probe.registerOnComplete();
            }
        };
    }

    @Override
    public Integer createElement(int element) {
        return element;
    }
}
