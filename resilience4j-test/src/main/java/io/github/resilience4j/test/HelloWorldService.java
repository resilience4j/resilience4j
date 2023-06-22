/*
 *
 *  Copyright 2016 Robert Winkler
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package io.github.resilience4j.test;

import io.vavr.control.Either;
import io.vavr.control.Try;

import java.io.IOException;
import java.util.concurrent.Future;
import java.net.http.HttpResponse;

public interface HelloWorldService {

    HttpResponse<String> returnHelloWorldResponse();

    String returnHelloWorld();

    Future<String> returnHelloWorldFuture();

    Either<HelloWorldException, String> returnEither();

    Try<String> returnTry();

    String returnHelloWorldWithException() throws IOException;

    String returnHelloWorldWithName(String name);

    String returnHelloWorldWithNameWithException(String name) throws IOException;

    void sayHelloWorld();

    void sayHelloWorldWithException() throws IOException;

    void sayHelloWorldWithName(String name);

    void sayHelloWorldWithNameWithException(String name) throws IOException;
}
