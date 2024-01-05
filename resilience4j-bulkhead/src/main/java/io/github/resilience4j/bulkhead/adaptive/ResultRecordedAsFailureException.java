package io.github.resilience4j.bulkhead.adaptive;

/**
 * A {@link ResultRecordedAsFailureException} signals that a result has been recorded as a adaptive bulkhead failure.
 */
public class ResultRecordedAsFailureException extends RuntimeException {

    private final String adaptiveBulkheadName;

    private final transient Object result;

    public ResultRecordedAsFailureException(String adaptiveBulkheadName, Object result) {
        super(String.format("AdaptiveBulkhead '%s' has recorded '%s' as a failure", adaptiveBulkheadName, result));
        this.result = result;
        this.adaptiveBulkheadName = adaptiveBulkheadName;
    }

    public Object getResult() {
        return result;
    }

    public String getAdaptiveBulkheadName() {
        return adaptiveBulkheadName;
    }

}
