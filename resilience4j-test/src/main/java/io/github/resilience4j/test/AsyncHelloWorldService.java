package io.github.resilience4j.test;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;

public interface AsyncHelloWorldService {
    CompletionStage<String> returnHelloWorld();
    Future<String> returnHelloWorldFuture();
    CompletionStage<String> returnHelloWorldWithName(String name);

    CompletionStage<Void> sayHelloWorld();
	Future<Void> sayHelloWorldFuture();
    CompletionStage<Void> sayHelloWorldWithName(String name);
}
