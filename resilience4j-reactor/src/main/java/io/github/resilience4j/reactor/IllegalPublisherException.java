package io.github.resilience4j.reactor;

import org.reactivestreams.Publisher;

public class IllegalPublisherException extends IllegalStateException {

    public IllegalPublisherException(Publisher publisher) {
        super("Publisher of type <" + publisher.getClass().getSimpleName()
                + "> is not supported by this operator");
    }
}
