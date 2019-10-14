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
