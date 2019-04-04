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
package io.github.resilience4j.reactor;

import io.github.resilience4j.core.lang.Nullable;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.Disposable;
import reactor.core.Exceptions;
import reactor.core.publisher.Operators;
import reactor.core.publisher.SignalType;
import reactor.util.context.Context;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * Heavily inspired by {@link reactor.core.publisher.BaseSubscriber}
 *
 * @param <T>
 */
public abstract class ResilienceBaseSubscriber<T> implements CoreSubscriber<T>, Subscription,
        Disposable {

    protected final CoreSubscriber<? super T> actual;

    @Nullable
    private volatile Subscription subscription;

    private static final AtomicReferenceFieldUpdater<ResilienceBaseSubscriber, Subscription> S =
            AtomicReferenceFieldUpdater.newUpdater(ResilienceBaseSubscriber.class, Subscription.class, "subscription");

    private final AtomicReference<Permit> permitted = new AtomicReference<>(Permit.PENDING);

    protected ResilienceBaseSubscriber(CoreSubscriber<? super T> actual) {
        this.actual = actual;
    }

    /**
     * Return current {@link Subscription}
     * @return current {@link Subscription}
     */

    @Nullable
    protected Subscription upstream() {
        return subscription;
    }

    @Override
    public boolean isDisposed() {
        return subscription == Operators.cancelledSubscription();
    }

    /**
     * {@link Disposable#dispose() Dispose} the {@link Subscription} by
     * {@link Subscription#cancel() cancelling} it.
     */
    @Override
    public void dispose() {
        cancel();
    }

    @Override
    public Context currentContext() {
        return actual.currentContext();
    }

    protected boolean notCancelled() {
        return !this.isDisposed();
    }

    /**
     * Hook for further processing of onSubscribe's Subscription.
     *
     * @param subscription the subscription to optionally process
     */
    protected void hookOnSubscribe(Subscription subscription){
        // NO-OP
    }

    /**
     * Hook for processing of onNext values. You can call {@link #request(long)} here
     * to further request data from the source {@link org.reactivestreams.Publisher} if
     * the {@link #hookOnSubscribe(Subscription) initial request} wasn't unbounded.
     * <p>Defaults to doing nothing.
     *
     * @param value the emitted value to process
     */
    protected void hookOnNext(T value){
        // NO-OP
    }

    /**
     * Optional hook for completion processing. Defaults to doing nothing.
     */
    protected void hookOnComplete() {
        // NO-OP
    }

    /**
     * Optional hook for error processing. Default is to call
     * {@link Exceptions#errorCallbackNotImplemented(Throwable)}.
     *
     * @param throwable the error to process
     */
    protected void hookOnError(Throwable throwable) {
        throw Exceptions.errorCallbackNotImplemented(throwable);
    }

    /**
     * Optional hook executed when the subscription is cancelled by calling this
     * Subscriber's {@link #cancel()} method. Defaults to doing nothing.
     */
    protected void hookOnCancel() {
        //NO-OP
    }

    /**
     * Optional hook executed after any of the termination events (onError, onComplete,
     * cancel). The hook is executed in addition to and after {@link #hookOnError(Throwable)},
     * {@link #hookOnComplete()} and {@link #hookOnCancel()} hooks, even if these callbacks
     * fail. Defaults to doing nothing. A failure of the callback will be caught by
     * {@link Operators#onErrorDropped(Throwable, reactor.util.context.Context)}.
     *
     * @param type the type of termination event that triggered the hook
     * ({@link SignalType#ON_ERROR}, {@link SignalType#ON_COMPLETE} or
     * {@link SignalType#CANCEL})
     */
    protected void hookFinally(SignalType type) {
        //NO-OP
    }

    /**
     * Optional hook executed when permit call is acquired.
     */
    protected void hookOnPermitAcquired() {
        //NO-OP
    }

    /**
     * @return true if call is permitted, false otherwise
     */
    protected abstract boolean isCallPermitted();

    protected boolean acquireCallPermit() {
        boolean callPermitted = false;
        if (permitted.compareAndSet(Permit.PENDING, Permit.ACQUIRED)) {
            callPermitted = isCallPermitted();
            if (!callPermitted) {
                permitted.set(Permit.REJECTED);
            } else {
                hookOnPermitAcquired();
            }
        }
        return callPermitted;
    }

    protected boolean wasCallPermitted() {
        return permitted.get() == Permit.ACQUIRED;
    }

    protected abstract Throwable getThrowable();

    @Override
    public final void onSubscribe(Subscription s) {
        if (Operators.setOnce(S, this, s)) {
            try {
                hookOnSubscribe(s);
                if (acquireCallPermit()) {
                    actual.onSubscribe(this);
                } else {
                    cancel();
                    actual.onSubscribe(this);
                    actual.onError(getThrowable());
                }
            }
            catch (Throwable throwable) {
                onError(Operators.onOperatorError(s, throwable, currentContext()));
            }
        }
    }

    @Override
    public final void onNext(T value) {
        Objects.requireNonNull(value, "onNext");
        try {
            hookOnNext(value);
        }
        catch (Throwable throwable) {
            onError(Operators.onOperatorError(subscription, throwable, value, currentContext()));
        }
    }

    @Override
    public final void onError(Throwable t) {
        Objects.requireNonNull(t, "onError");

        if (S.getAndSet(this, Operators.cancelledSubscription()) == Operators
                .cancelledSubscription()) {

            // already cancelled concurrently...
            if (permitted.get() == Permit.REJECTED) {
                // Ignore if the call was rejected
                return;
            }

            // signal on error dropped otherwise
            Operators.onErrorDropped(t, currentContext());
            return;
        }


        try {
            hookOnError(t);
        }
        catch (Throwable e) {
            e = Exceptions.addSuppressed(e, t);
            Operators.onErrorDropped(e, currentContext());
        }
        finally {
            safeHookFinally(SignalType.ON_ERROR);
        }
    }

    @Override
    public final void onComplete() {
        if (S.getAndSet(this, Operators.cancelledSubscription()) != Operators
                .cancelledSubscription()) {
            //we're sure it has not been concurrently cancelled
            try {
                hookOnComplete();
            }
            catch (Throwable throwable) {
                //onError itself will short-circuit due to the CancelledSubscription being push above
                hookOnError(Operators.onOperatorError(throwable, currentContext()));
            }
            finally {
                safeHookFinally(SignalType.ON_COMPLETE);
            }
        }
    }

    @Override
    public final void request(long n) {
        if (Operators.validate(n)) {
            Subscription s = this.subscription;
            if (s != null) {
                s.request(n);
            }
        }
    }

    /**
     * {@link #request(long) Request} an unbounded amount.
     */
    public final void requestUnbounded() {
        request(Long.MAX_VALUE);
    }

    @Override
    public final void cancel() {
        if (Operators.terminate(S, this)) {
            try {
                hookOnCancel();
            }
            catch (Throwable throwable) {
                hookOnError(Operators.onOperatorError(subscription, throwable, currentContext()));
            }
            finally {
                safeHookFinally(SignalType.CANCEL);
            }
        }
    }

    private void safeHookFinally(SignalType type) {
        try {
            hookFinally(type);
        }
        catch (Throwable finallyFailure) {
            Operators.onErrorDropped(finallyFailure, currentContext());
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
