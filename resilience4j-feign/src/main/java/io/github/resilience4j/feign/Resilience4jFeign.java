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
import feign.Target;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Main class for combining feign with Resilience4j.
 *
 * <pre>
 * {@code
 *     MyService myService = Feign.builder()
 *                              .addCapability(Resilience4jFeign.capability(decorators)
 *                              .target(MyService.class, "http://localhost:8080/");
 * }
 * </pre>
 * <p>
 * {@link Resilience4jFeign.Builder} builder works in the same way as the standard {@link Feign.Builder}. (10.9 - 12.4),
 * {@link Resilience4jFeign.Capability} for 12.5+ you must use Resilience4jFeign.Capability instead
 * this is how {@link Resilience4jFeign} decorates the feign interface. <br> See {@link
 * FeignDecorators} on how to build decorators and enhance your feign interfaces.
 */
public final class Resilience4jFeign {

    /**
     * @deprecated Feign 12.5+ is not compatible, please use Resilience4jFeign.capability() instead
     */
    @Deprecated
    public static Builder builder(FeignDecorator invocationDecorator) {
        Builder builder = new Builder();
        builder.addCapability(Resilience4jFeign.capability(invocationDecorator));
        return builder;
    }

    public static Capability capability(FeignDecorator invocationDecorator) {
        return new Capability(invocationDecorator);
    }

    public static final class Builder extends Feign.Builder {
    }

    public static final class InvocationHandlerFactory implements feign.InvocationHandlerFactory {
        private final FeignDecorator feignDecorator;

        public InvocationHandlerFactory(FeignDecorator feignDecorator) {
            this.feignDecorator = feignDecorator;
        }

        @Override
        public InvocationHandler create(Target target, Map<Method, MethodHandler> dispatch) {
            return new DecoratorInvocationHandler(target, dispatch, feignDecorator);
        }
    }

    public static final class Capability implements feign.Capability {
        private final FeignDecorator feignDecorator;

        public Capability(FeignDecorator feignDecorator) {
            this.feignDecorator = feignDecorator;
        }

        @Override
        public feign.InvocationHandlerFactory enrich(feign.InvocationHandlerFactory invocationHandlerFactory) {
            return new InvocationHandlerFactory(feignDecorator);
        }
    }
}
