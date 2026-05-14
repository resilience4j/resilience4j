package io.github.resilience4j;

import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.TestScheduler;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class TestSchedulerExtension implements BeforeEachCallback, AfterEachCallback {

    private static final ExtensionContext.Namespace NAMESPACE =
        ExtensionContext.Namespace.create(TestSchedulerExtension.class);

    @Override
    public void beforeEach(ExtensionContext context) {
        TestScheduler testScheduler = new TestScheduler();
        context.getStore(NAMESPACE).put("testScheduler", testScheduler);
        RxJavaPlugins.setIoSchedulerHandler(scheduler -> testScheduler);
        RxJavaPlugins.setComputationSchedulerHandler(scheduler -> testScheduler);
        RxJavaPlugins.setNewThreadSchedulerHandler(scheduler -> testScheduler);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        RxJavaPlugins.reset();
    }

    public static TestScheduler getTestScheduler(ExtensionContext context) {
        return context.getStore(NAMESPACE).get("testScheduler", TestScheduler.class);
    }
}
