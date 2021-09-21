package io.github.resilience4j.prometheus;

import io.github.resilience4j.core.functions.CheckedFunction;
import io.github.resilience4j.core.functions.CheckedRunnable;
import io.github.resilience4j.core.functions.CheckedSupplier;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;

public interface CallMeter extends CallMeterBase {

    /**
     * Creates call meter with the given name and help message
     *
     * @param name - metric name
     * @param help - metric help
     * @return the call meter
     */
    static CallMeter of(String name, String help) {
        return CallMeter
            .builder()
            .name(name)
            .help(help)
            .build();
    }

    /**
     * Creates call meter with the given name and registers it in the specified collector registry
     *
     * @param name     - metric name
     * @param help     - metric help
     * @param registry - collector registry
     * @return the call meter
     */
    static CallMeter ofCollectorRegistry(String name, String help, CollectorRegistry registry) {
        return of(name, help).register(registry);
    }

    /**
     * Creates a new call meter {@link Builder}
     *
     * @return the new {@link Builder}
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a timed checked supplier.
     *
     * @param meter    the call meter to use
     * @param supplier the original supplier
     * @return a timed supplier
     */
    static <T> CheckedSupplier<T> decorateCheckedSupplier(CallMeterBase meter,
                                                          CheckedSupplier<T> supplier) {
        return () -> {
            final Timer timer = meter.startTimer();
            try {
                final T returnValue = supplier.get();
                timer.onSuccess();
                return returnValue;
            } catch (Throwable e) {
                timer.onError();
                throw e;
            }
        };
    }

    /**
     * Creates a timed runnable.
     *
     * @param meter    the call meter to use
     * @param runnable the original runnable
     * @return a timed runnable
     */
    static CheckedRunnable decorateCheckedRunnable(CallMeterBase meter, CheckedRunnable runnable) {
        return () -> {
            final Timer timer = meter.startTimer();
            try {
                runnable.run();
                timer.onSuccess();
            } catch (Throwable e) {
                timer.onError();
                throw e;
            }
        };
    }

    /**
     * Creates a timed checked supplier.
     *
     * @param meter    the call meter to use
     * @param supplier the original supplier
     * @return a timed supplier
     */
    static <T> Supplier<T> decorateSupplier(CallMeterBase meter, Supplier<T> supplier) {
        return () -> {
            final Timer timer = meter.startTimer();
            try {
                final T returnValue = supplier.get();
                timer.onSuccess();
                return returnValue;
            } catch (Throwable e) {
                timer.onError();
                throw e;
            }
        };
    }

    /**
     * Creates a timed Callable.
     *
     * @param meter    the call meter to use
     * @param callable the original Callable
     * @return a timed Callable
     */
    static <T> Callable<T> decorateCallable(CallMeterBase meter, Callable<T> callable) {
        return () -> {
            final Timer timer = meter.startTimer();
            try {
                final T returnValue = callable.call();
                timer.onSuccess();
                return returnValue;
            } catch (Throwable e) {
                timer.onError();
                throw e;
            }
        };
    }

    /**
     * Creates a timed runnable.
     *
     * @param meter    the call meter to use
     * @param runnable the original runnable
     * @return a timed runnable
     */
    static Runnable decorateRunnable(CallMeterBase meter, Runnable runnable) {
        return () -> {
            final Timer timer = meter.startTimer();
            try {
                runnable.run();
                timer.onSuccess();
            } catch (Throwable e) {
                timer.onError();
                throw e;
            }
        };
    }

    /**
     * Creates a timed function.
     *
     * @param meter    the call meter to use
     * @param function the original function
     * @return a timed function
     */
    static <T, R> Function<T, R> decorateFunction(CallMeterBase meter, Function<T, R> function) {
        return (T t) -> {
            final Timer timer = meter.startTimer();
            try {
                R returnValue = function.apply(t);
                timer.onSuccess();
                return returnValue;
            } catch (Throwable e) {
                timer.onError();
                throw e;
            }
        };
    }

    /**
     * Creates a timed function.
     *
     * @param meter    the call meter to use
     * @param function the original function
     * @return a timed function
     */
    static <T, R> CheckedFunction<T, R> decorateCheckedFunction(CallMeterBase meter,
                                                                CheckedFunction<T, R> function) {
        return (T t) -> {
            final Timer timer = meter.startTimer();
            try {
                R returnValue = function.apply(t);
                timer.onSuccess();
                return returnValue;
            } catch (Throwable e) {
                timer.onError();
                throw e;
            }
        };
    }

    /**
     * Decorates completion stage supplier with call meter
     *
     * @param meter         the call meter to use
     * @param stageSupplier the CompletionStage Supplier
     * @return a decorated completion stage
     */
    static <T> Supplier<CompletionStage<T>> decorateCompletionStageSupplier(CallMeterBase meter,
        Supplier<CompletionStage<T>> stageSupplier) {
        return () -> {
            final Timer timer = meter.startTimer();
            try {
                final CompletionStage<T> stage = stageSupplier.get();

                stage.whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        timer.onError();
                    } else {
                        timer.onSuccess();
                    }
                });

                return stage;
            } catch (Throwable throwable) {
                timer.onError();
                throw throwable;
            }
        };
    }

    /**
     * Creates a child call meter with the given labels
     *
     * @param labels
     * @return child collector
     */
    Child labels(String... labels);

    /**
     * Register this call meter with the default registry.
     */
    default CallMeter register() {
        return register(CollectorRegistry.defaultRegistry);
    }

    /**
     * Registers this call meter with the given registry.
     */
    CallMeter register(CollectorRegistry registry);

    interface Child extends CallMeterBase {

    }

    class Builder {

        private String namespace = "";
        private String subsystem = "";
        private String name = "";
        private String help = "";
        private String[] labelNames = new String[]{};

        /**
         * Set the name of the metric. Required.
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Set the subsystem of the metric. Optional.
         */
        public Builder subsystem(String subsystem) {
            this.subsystem = subsystem;
            return this;
        }

        /**
         * Set the namespace of the metric. Optional.
         */
        public Builder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        /**
         * Set the help string of the metric. Required.
         */
        public Builder help(String help) {
            this.help = help;
            return this;
        }

        /**
         * Set the labelNames of the metric. Optional, defaults to no labels.
         */
        public Builder labelNames(String... labelNames) {
            this.labelNames = labelNames;
            return this;
        }

        /**
         * Return the constructed collector.
         */
        public CallMeter build() {
            return new CallMeterImpl(createMetrics());
        }

        private CallCollectors createMetrics() {
            final Counter totalCounter = Counter
                .build()
                .namespace(namespace)
                .subsystem(subsystem)
                .name(name + "_total")
                .help(help + " total")
                .labelNames(labelNames)
                .create();

            final Counter errorCounter = Counter
                .build()
                .namespace(namespace)
                .subsystem(subsystem)
                .name(name + "_failures_total")
                .help(help + " failures total")
                .labelNames(labelNames)
                .create();

            final Histogram histogram = Histogram
                .build()
                .namespace(namespace)
                .subsystem(subsystem)
                .name(name + "_latency")
                .help(help + " latency")
                .labelNames(labelNames)
                .create();

            return new CallCollectors(histogram, totalCounter, errorCounter);
        }
    }
}

