/*
 * Copyright 2026 Bobae Kim
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.spring6.httpservice.test;

/**
 * A fallback consuming the thrown exception.
 */
public class TestHttpServiceFallbackWithException implements TestHttpService {

    private final Exception cause;

    public TestHttpServiceFallbackWithException(Exception cause) {
        this.cause = cause;
    }

    @Override
    public String greeting() {
        return "Message from exception: " + cause.getMessage();
    }

    @Override
    public String greetingWithName(String name) {
        return "Fallback with exception for: " + name + ", cause: " + cause.getMessage();
    }

    @Override
    public String echo(String message) {
        return "Echo fallback with exception: " + message + ", cause: " + cause.getMessage();
    }
}
