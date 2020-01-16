package io.github.resilience4j.retry.event;

public class SuccessWithoutErrorEvent extends AbstractRetryEvent  {

    public SuccessWithoutErrorEvent(String name) {
        super(name, 0, null);
    }

    @Override
    public Type getEventType() {
        return Type.SUCCESS_WITHOUT_RETRY;
    }

    @Override
    public String toString() {
        return String.format(
            "%s: Retry '%s' recorded a successful without retry attempt. Number of retry attempts: '0', Last exception was: 'null'.",
            getCreationTime(),
            getName());
    }
}
