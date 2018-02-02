/*
 *
 * Copyright 2018
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 *
 */
package io.github.resilience4j.feign;

import feign.Feign;
import feign.InvocationHandlerFactory;

/**
 * Main class for combining Feign with Resilience4j modules. See {@link FeignDecorators} on how to
 * build decorators.
 */
public final class Resilience4jFeign {

    public static Builder builder(FeignDecorator invocationDecorator) {
        return new Builder(invocationDecorator);
    }

    public static final class Builder extends Feign.Builder {

        private final FeignDecorator invocationDecorator;

        public Builder(FeignDecorator invocationDecorator) {
            this.invocationDecorator = invocationDecorator;
        }

        @Override
        public Feign.Builder invocationHandlerFactory(InvocationHandlerFactory invocationHandlerFactory) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Feign build() {
            super.invocationHandlerFactory(
                    (target, dispatch) -> new DecoratorInvocationHandler(target, dispatch, invocationDecorator));
            return super.build();
        }

    }
}
