package io.github.resilience4j.circuitbreaker.test;

import io.vertx.core.Future;

public interface VertxHelloWorldService {

    Future<String> returnHelloWorld();
}
