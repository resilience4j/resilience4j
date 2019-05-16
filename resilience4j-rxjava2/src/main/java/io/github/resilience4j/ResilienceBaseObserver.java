package io.github.resilience4j;

import io.github.resilience4j.adapter.Permit;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;

import java.util.concurrent.atomic.AtomicReference;

public abstract class ResilienceBaseDisposable extends AtomicReference<Disposable> implements Disposable {

    private final AtomicReference<Permit> permitted = new AtomicReference<>(Permit.PENDING);

    @Override
    public void dispose() {
        if (DisposableHelper.dispose(this)) {
            hookOnDispose();
        }
    }

    protected void hookOnDispose() {

    }

    @Override
    public boolean isDisposed() {
        return DisposableHelper.isDisposed(get());
    }

    protected final void onSubscribeWithPermit(Disposable disposable) {
        if (DisposableHelper.setOnce(this, disposable)) {
            if (acquireCallPermit()) {
                hookOn
            } else {
                disposable.dispose();
                onSubscribeInner(this);
                permittedOnError(bulkheadFullException());
            }
        }
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
    protected abstract boolean obtainPermission();

    private boolean acquireCallPermit() {
        boolean callPermitted = false;
        if (permitted.compareAndSet(Permit.PENDING, Permit.ACQUIRED)) {
            callPermitted = obtainPermission();
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
}
