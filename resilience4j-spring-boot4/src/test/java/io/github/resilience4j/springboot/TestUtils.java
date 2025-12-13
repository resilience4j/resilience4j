package io.github.resilience4j.springboot;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;


public class TestUtils {

    public static void assertAnnotations(Class<?> originalClass, Class<?> autoConfigurationClass)
        throws NoSuchMethodException {
        for (Method originalMethod : originalClass.getMethods()) {
            if (originalMethod.isAnnotationPresent(Bean.class)) {
                Method autoConfigurationMethod = autoConfigurationClass
                    .getMethod(originalMethod.getName(), originalMethod.getParameterTypes());

                assertThat(autoConfigurationMethod.isAnnotationPresent(Bean.class)).isTrue();

                if (autoConfigurationMethod.getName().endsWith("RegistryEventConsumer")
                        || autoConfigurationMethod.getName().startsWith("composite")) {
                    continue;
                }

                assertThat(autoConfigurationMethod.isAnnotationPresent(ConditionalOnMissingBean.class))
                        .withFailMessage("%s.%s must be annotated with @ConditionalOnMissingBean"
                                .formatted(originalClass.getName(), autoConfigurationMethod.getName()))
                        .isTrue();

            }
        }
    }
}
