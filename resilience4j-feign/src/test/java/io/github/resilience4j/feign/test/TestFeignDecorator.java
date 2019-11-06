package io.github.resilience4j.feign.test;

import feign.InvocationHandlerFactory.MethodHandler;
import feign.Target;
import io.github.resilience4j.feign.FeignDecorator;
import io.vavr.CheckedFunction1;

import java.lang.reflect.Method;

public class TestFeignDecorator implements FeignDecorator {

    private volatile boolean called = false;
    private volatile CheckedFunction1<Object[], Object> alternativeFunction;

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
    public CheckedFunction1<Object[], Object> decorate(
        CheckedFunction1<Object[], Object> invocationCall,
        Method method, MethodHandler methodHandler,
        Target<?> target) {
        called = true;
        return alternativeFunction != null ? alternativeFunction : invocationCall;
    }

}
