package io.github.resilience4j.reactor.circuitbreaker.operator;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.SubscriberWhiteboxVerification;
import org.reactivestreams.tck.TestEnvironment;
import reactor.core.publisher.MonoProcessor;

public class CircuitBreakerSubscriberWhiteboxVerification extends
    SubscriberWhiteboxVerification<Integer> {

  public CircuitBreakerSubscriberWhiteboxVerification() {
    super(new TestEnvironment());
  }

  @Override
  public Subscriber<Integer> createSubscriber(WhiteboxSubscriberProbe<Integer> probe) {
    return new CircuitBreakerSubscriber<Integer>(CircuitBreaker.ofDefaults("verification"), MonoProcessor.create()) {
      @Override
      public void onSubscribe(Subscription subscription) {
        super.onSubscribe(subscription);

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
      public void onNext(Integer integer) {
        super.onNext(integer);
        probe.registerOnNext(integer);
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
