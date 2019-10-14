/*
 *  Copyright 2019 Marco Ferrer
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
 */
package io.github.resilience4j.grpc.circuitbreaker.server;

import io.grpc.BindableService;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;

public interface CircuitBreakerServerDecorators {

    public static ServerCircuitBreakerDecorator forServiceDefinition(ServerServiceDefinition serviceDef){
        return new ServerServiceDefinitionDecorator(serviceDef);
    }

    public static BindableServiceDecorator forBindableService(BindableService bindableService){
        return new BindableServiceDecorator(bindableService);
    }

    public class BindableServiceDecorator extends ServerCircuitBreakerDecorator<BindableServiceDecorator> {

        private final BindableService bindableService;

        private BindableServiceDecorator(BindableService bindableService) {
            this.bindableService = bindableService;
        }

        public ServerServiceDefinition build(){
            return ServerInterceptors.interceptForward(bindableService, interceptors);
        }
    }

    public class ServerServiceDefinitionDecorator extends ServerCircuitBreakerDecorator<ServerServiceDefinitionDecorator> {

        private final ServerServiceDefinition serverServiceDefinition;

        private ServerServiceDefinitionDecorator(ServerServiceDefinition serverServiceDefinition) {
            this.serverServiceDefinition = serverServiceDefinition;
        }

        public ServerServiceDefinition build(){
            return ServerInterceptors.interceptForward(serverServiceDefinition, interceptors);
        }
    }
}
