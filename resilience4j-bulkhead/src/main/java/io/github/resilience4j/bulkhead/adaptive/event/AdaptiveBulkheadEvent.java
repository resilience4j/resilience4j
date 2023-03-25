package io.github.resilience4j.bulkhead.adaptive.event;

import java.time.ZonedDateTime;

/**
 * adaptive bulkhead event
 */
public interface AdaptiveBulkheadEvent {
    /**
     * Returns the name of the bulkhead which has created the event.
     *
     * @return the name of the bulkhead which has created the event
     */
    String getBulkheadName();

    /**
     * Returns the type of the bulkhead event.
     *
     * @return the type of the bulkhead event
     */
    Type getEventType();


    /**
     * Returns the creation time of adaptive bulkhead event.
     *
     * @return the creation time of adaptive bulkhead event
     */
    ZonedDateTime getCreationTime();


    /**
     * Event types which are created by a bulkhead.
     */
    enum Type {
        /**
         * A AdaptiveBulkheadEvent which informs that a limit has been increased
         */
        LIMIT_INCREASED(false),
        /**
         * A AdaptiveBulkheadEvent which informs that a limit has been decreased
         */
        LIMIT_DECREASED(false),
        /**
         * An adaptive bulkhead event which informs that an error has been recorded
         */
        ERROR(false),
        /**
         * An adaptive bulkhead  which informs that an error has been ignored
         */
        IGNORED_ERROR(false),
        /**
         * An adaptive bulkhead  which informs that a success has been recorded
         */
        SUCCESS(false),

        /**
         * A AdaptiveBulkheadEvent which informs the state of the AdaptiveBulkhead has been changed
         */
        STATE_TRANSITION(true),
        /**
         * A AdaptiveBulkheadEvent which informs the AdaptiveBulkhead has been reset
         */
        RESET(true),
        /**
         * A AdaptiveBulkheadEvent which informs the AdaptiveBulkhead has been disabled
         */
        DISABLED(false);

        public final boolean forcePublish;

        Type(boolean forcePublish) {
            this.forcePublish = forcePublish;
        }
    }

}
