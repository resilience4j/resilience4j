package io.github.resilience4j.rxjava3;

import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class TestSchedulerRule implements TestRule {

    private final TestScheduler testScheduler = new TestScheduler();

    public TestScheduler getTestScheduler() {
        return testScheduler;
    }

    @Override
    public Statement apply(final Statement statement, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                RxJavaPlugins.setIoSchedulerHandler(scheduler -> testScheduler);
                RxJavaPlugins.setComputationSchedulerHandler(scheduler -> testScheduler);
                RxJavaPlugins.setNewThreadSchedulerHandler(scheduler -> testScheduler);
                try {
                    statement.evaluate();
                } finally {
                    RxJavaPlugins.reset();
                }
            }
        };
    }
}
