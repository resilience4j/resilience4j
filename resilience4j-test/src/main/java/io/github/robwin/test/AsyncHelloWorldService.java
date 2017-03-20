package io.github.robwin.test;

import java.util.concurrent.CompletionStage;

public interface AsyncHelloWorldService {
    CompletionStage<String> returnHelloWorld();
    CompletionStage<String> returnHelloWorldWithName(String name);

    CompletionStage<Void> sayHelloWorld();
    CompletionStage<Void> sayHelloWorldWithName(String name);
}
