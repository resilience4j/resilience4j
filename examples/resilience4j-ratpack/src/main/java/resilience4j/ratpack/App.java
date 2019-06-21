package resilience4j.ratpack;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratpack.Resilience4jConfig;
import io.github.resilience4j.ratpack.Resilience4jModule;
import ratpack.guice.Guice;
import ratpack.server.RatpackServer;

public class App {

    public static void main(String[] args) throws Exception {
        RatpackServer.start(s -> s
                .serverConfig(c -> c
                        .development(true)
                        .yaml(App.class.getClassLoader().getResource("application.yml"))
                        .require("/resilience4j", Resilience4jConfig.class)
                )
                .registry(Guice.registry(b -> b
                        .module(Resilience4jModule.class)
//                        .module(DropwizardMetricsModule.class)
                        .bind(Something.class)
                ))
                .handlers(c -> c
                        .get("a", context -> {
                            Something something = context.get(Something.class);
                            context.render(something.a());
                        })
                        .get("b", context -> {
                            Something something = context.get(Something.class);
                            context.render(something.b());
                        })
                        .get("c", context -> {
                            Something something = context.get(Something.class);
                            context.render(something.c());
                        })
                    )
        );
    }

    public static class Something {

        @CircuitBreaker(name = "test1")
        public String a() {
            return "a";
        }

        @CircuitBreaker(name = "test1")
        public String b() throws DummyException1 {
            throw new DummyException1("b");
        }

        @CircuitBreaker(name = "test2")
        public String c() throws DummyException1 {
            throw new DummyException1("c");
        }

    }

    public static class DummyException1 extends Exception {
        DummyException1(String message) {
            super(message);
        }
    }

    public static class DummyException2 extends Exception {
        DummyException2(String message) {
            super(message);
        }
    }

}
