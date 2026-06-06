package io.github.resilience4j.core;

import org.junit.jupiter.api.extension.*;

import java.util.List;
import java.util.stream.Stream;

/**
 * JUnit 5 extension that runs each {@code @TestTemplate} method once per {@link ThreadType}.
 *
 * <p>Usage:
 * <pre>{@code
 * @ExtendWith(ThreadModeExtension.class)
 * class MyTest {
 *
 *     @TestTemplate
 *     void myTest(ThreadType threadType) {
 *         // threadType injected; system property already set; restored automatically after
 *     }
 * }
 * }</pre>
 *
 * <p>The extension:
 * <ul>
 *   <li>Saves the original {@code resilience4j.thread.type} system property before each invocation</li>
 *   <li>Sets it to the appropriate value for the current {@link ThreadType}</li>
 *   <li>Restores the original value after the invocation</li>
 *   <li>Injects the {@link ThreadType} parameter into the test method if declared</li>
 * </ul>
 */
public class ThreadModeExtension implements TestTemplateInvocationContextProvider {

    private static final String SYS_PROP_KEY = "resilience4j.thread.type";
    private static final String STORE_KEY = "originalThreadTypeProp";

    @Override
    public boolean supportsTestTemplate(ExtensionContext context) {
        return true;
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
        return Stream.of(ThreadType.values()).map(ThreadModeInvocationContext::new);
    }

    private static class ThreadModeInvocationContext implements TestTemplateInvocationContext {

        private final ThreadType threadType;

        ThreadModeInvocationContext(ThreadType threadType) {
            this.threadType = threadType;
        }

        @Override
        public String getDisplayName(int invocationIndex) {
            return "[" + threadType + " thread mode]";
        }

        @Override
        public List<Extension> getAdditionalExtensions() {
            return List.of(
                (BeforeEachCallback) ctx -> {
                    // Save original property value
                    String original = System.getProperty(SYS_PROP_KEY);
                    ctx.getStore(ExtensionContext.Namespace.create(ThreadModeExtension.class))
                        .put(STORE_KEY, original != null ? original : "");

                    // Set property for this thread mode
                    if (threadType == ThreadType.VIRTUAL) {
                        System.setProperty(SYS_PROP_KEY, threadType.toString());
                    } else {
                        System.clearProperty(SYS_PROP_KEY);
                    }
                },
                (AfterEachCallback) ctx -> {
                    // Restore original property value
                    String original = ctx.getStore(ExtensionContext.Namespace.create(ThreadModeExtension.class))
                        .get(STORE_KEY, String.class);
                    if (original != null && !original.isEmpty()) {
                        System.setProperty(SYS_PROP_KEY, original);
                    } else {
                        System.clearProperty(SYS_PROP_KEY);
                    }
                },
                new ThreadTypeParameterResolver(threadType)
            );
        }
    }

    private static class ThreadTypeParameterResolver implements ParameterResolver {

        private final ThreadType threadType;

        ThreadTypeParameterResolver(ThreadType threadType) {
            this.threadType = threadType;
        }

        @Override
        public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
            return parameterContext.getParameter().getType() == ThreadType.class;
        }

        @Override
        public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
            return threadType;
        }
    }
}
