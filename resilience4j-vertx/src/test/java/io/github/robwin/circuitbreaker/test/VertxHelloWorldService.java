package io.github.robwin.circuitbreaker.test;

import io.vertx.core.Future;

public interface VertxHelloWorldService {
    Future<String> returnHelloWorld();
}
