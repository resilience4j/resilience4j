package io.github.resilience4j.feign;

import io.vavr.CheckedFunction1;

public class TestFeignDecorator implements FeignDecorator {

    private boolean called;
    private CheckedFunction1<Object[], Object> alternativeFunction;

    public boolean isCalled() {
        return called;
    }

    public void setCalled(boolean called) {
        this.called = called;
    }

    public CheckedFunction1<Object[], Object> getAlternativeFunction() {
        return alternativeFunction;
    }

    public void setAlternativeFunction(CheckedFunction1<Object[], Object> alternativeFunction) {
        this.alternativeFunction = alternativeFunction;
    }

    @Override
    public CheckedFunction1<Object[], Object> decorate(CheckedFunction1<Object[], Object> fn) {
        called = true;
        return alternativeFunction != null ? alternativeFunction : fn;
    }

}
