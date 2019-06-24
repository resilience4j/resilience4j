package io.github.resilience4j.bulkhead.event;

import java.util.Map;

/**
 * adaptive bulkhead event
 */
public interface BulkheadLimit {
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
	 * @return event related data like new max limit ..ect
	 */
	Map<String, String> eventData();


	/**
	 * Event types which are created by a bulkhead.
	 */
	enum Type {
		/**
		 * A BulkheadLimit which informs that a limit has been increased
		 */
		LIMIT_INCREASED,
		/**
		 * A BulkheadLimit which informs that a limit has been decreased
		 */
		LIMIT_DECREASED,

	}

}
