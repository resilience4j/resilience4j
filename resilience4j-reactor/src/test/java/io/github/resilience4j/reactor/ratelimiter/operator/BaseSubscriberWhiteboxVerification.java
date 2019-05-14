package io.github.resilience4j.reactor.ratelimiter.operator;

import io.github.resilience4j.ratelimiter.RateLimiter;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.SubscriberWhiteboxVerification;
import org.reactivestreams.tck.TestEnvironment;
import reactor.core.publisher.MonoProcessor;
import reactor.core.scheduler.Schedulers;

public abstract class BaseSubscriberWhiteboxVerification extends
        SubscriberWhiteboxVerification<Integer> {

    BaseSubscriberWhiteboxVerification() {
        super(new TestEnvironment());
    }

    protected abstract RateLimiter getRateLimiter();

    @Override
    public Subscriber<Integer> createSubscriber(WhiteboxSubscriberProbe<Integer> probe) {
        return new RateLimiterSubscriber<Integer>(MonoProcessor.create(), getRateLimiter(), Schedulers.parallel(), false) {
            @Override
            public void onSubscribe(Subscription s) {
                super.onSubscribe(s);
                // register a successful Subscription, and create a Puppet,
                // for the WhiteboxVerification to be able to drive its tests:
                probe.registerOnSubscribe(new SubscriberPuppet() {

                    @Override
                    public void triggerRequest(long elements) {
                        s.request(elements);
                    }

                    @Override
                    public void signalCancel() {
                        s.cancel();
                    }
                });
            }

            @Override
            public void onNext(Integer i) {
                super.onNext(i);
                probe.registerOnNext(i);
            }

            @Override
            public void onError(Throwable t) {
                super.onError(t);
                probe.registerOnError(t);
            }

            @Override
            public void onComplete() {
                super.onComplete();
                probe.registerOnComplete();
            }
        };
    }

    @Override
    public Integer createElement(int element) {
        return element;
    }
}
