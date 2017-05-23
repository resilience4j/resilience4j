package io.github.resilience4j.timeout.internal;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import io.github.resilience4j.timeout.TimeoutConfig;
import io.github.resilience4j.timeout.TimeoutException;
import io.vavr.control.Try;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;
import static io.vavr.Predicates.instanceOf;

/**
 * {@link FutureTimeoutProxy} redirections invocation to Future.get/0 and Future.get/2
 * to Future.get/2 with the timeout specific in {@link TimeoutConfig}.
 */
public class FutureTimeoutProxy<T extends Future> implements InvocationHandler {

    // Preloaded Methods
    private static Method getMethod;
    private static Method getWithTimeoutMethod;
    static {
        try {
            Class<?> params[] = new Class[2];
            params[0] = long.class;
            params[1] = TimeUnit.class;

            getMethod = Future.class.getMethod("get", null);
            getWithTimeoutMethod = Future.class.getMethod("get", params);
        } catch (NoSuchMethodException e) {
            throw new NoSuchMethodError(e.getMessage());
        }
    }

    private T t;
    private Duration duration;

    public FutureTimeoutProxy(T t, Duration duration) {
        this.t = t;
        this.duration = duration;
    }

    /**
     * Proxies the invocation of Future.get methods using the configured timeout duration.
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.equals(getMethod) || method.equals(getWithTimeoutMethod)) {
            Object[] newArgs = new Object[2];
            newArgs[0] = duration.toMillis();
            newArgs[1] = TimeUnit.MILLISECONDS;
            return Try.of(() -> getWithTimeoutMethod.invoke(t, newArgs))
                    .getOrElseThrow(throwable -> Match(throwable).of(
                            Case($(instanceOf(InvocationTargetException.class)), t -> new TimeoutException(t.getTargetException())),
                            Case($(), TimeoutException::new))
                    );
        }
        return method.invoke(t, args);
    }

    /**
     * Creates a proxied object of type T
     * @param future          the Future to proxy
     * @param timeoutDuration the timeout duration
     * @param <T>             the Future type
     * @return a proxied Future of type T
     */
    @SuppressWarnings("unchecked")
    public static <T extends Future> T getProxy(T future, Duration timeoutDuration) {
        FutureTimeoutProxy handler = new FutureTimeoutProxy(future, timeoutDuration);
        return (T) Proxy.newProxyInstance(future.getClass().getClassLoader(),
                new Class<?>[]{Future.class}, handler
        );
    }
}
