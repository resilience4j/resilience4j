package io.github.resilience4j.core.functions;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Function;

public final class CallsResult {

    @Nullable
    private final Throwable thrown;

    @Nullable
    private final Object returned;

    private CallsResult(@Nullable Throwable thrown, @Nullable Object returned) {
        this.thrown = thrown;
        this.returned = returned;
    }

    public static CallsResult failure(Throwable thrown) {
        if (thrown == null) {
            throw new IllegalArgumentException("thrown cannot be null");
        }
        return new CallsResult(thrown, null);
    }

    public static CallsResult success() {
        return new CallsResult(null, null);
    }

    public static CallsResult success(Object returned) {
        return new CallsResult(null, returned);
    }


    public <T> boolean isSuccessfulAndReturned(Class<T> expectedClass, Function<T, Boolean> returnedChecker) {
        if (isFailed() || returned == null) {
            return false;
        }
        if (!expectedClass.isAssignableFrom(returned.getClass())) {
            return false;
        }
        return returnedChecker.apply((T) returned);
    }

    public boolean isSuccessful() {
        return !isFailed();
    }

    public <T extends Throwable>  boolean isFailedAndThrown(Class<T> expectedClass) {
        return isFailedAndThrown(expectedClass, thrown -> true);
    }

    public <T extends Throwable>  boolean isFailedAndThrown(
            Class<T> expectedClass,
            Function<T, Boolean> thrownChecker) {
        if (isSuccessful()) {
            return false;
        }
        if (!expectedClass.isAssignableFrom(thrown.getClass())) {
            return false;
        }
        return thrownChecker.apply((T) thrown);
    }

    public boolean isFailed() {
        return getThrown().isPresent();
    }

    public Optional<Throwable> getThrown() {
        return Optional.ofNullable(thrown);
    }

    public Optional<Object> getReturned() {
        return Optional.ofNullable(returned);
    }
}
