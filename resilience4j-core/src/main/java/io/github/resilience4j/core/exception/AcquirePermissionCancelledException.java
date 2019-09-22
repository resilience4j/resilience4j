package io.github.resilience4j.core.exception;

/**
 * Exception indicating that the permission wasn't acquired because the
 * task was cancelled or thread interrupted.
 * <p>
 * We extend it from IllegalStateException to preserve backwards compatibility with version 1.0.0 of Resilience4j
 */
public class AcquirePermissionCancelledException extends IllegalStateException {
    /**
     * Constructs a {@code AcquirePermissionCancelledException} with detail message.
     *
     * @param message the detail message
     */
    public AcquirePermissionCancelledException(String message) {
        super(message);
    }
}
