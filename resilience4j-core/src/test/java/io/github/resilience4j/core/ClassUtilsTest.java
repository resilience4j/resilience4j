package io.github.resilience4j.core;

import org.junit.Test;

import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ClassUtilsTest {

    @Test
    public void shouldInstantiatePredicateClass() {
        assertThat(ClassUtils.instantiatePredicateClass(PublicPredicate.class)).isNotNull();
    }

    @Test
    public void shouldFailToInstantiatePredicateClass() {
        assertThatThrownBy(
            () -> ClassUtils.instantiatePredicateClass(NoDefaultConstructorPredicate.class))
            .isInstanceOf(InstantiationException.class)
            .hasCauseInstanceOf(NoSuchMethodException.class);
    }


    public static class PublicPredicate implements Predicate<String> {

        @Override
        public boolean test(String o) {
            return false;
        }
    }

    private static class NoDefaultConstructorPredicate implements Predicate<String> {

        private String bla;

        public NoDefaultConstructorPredicate(String bla) {

            this.bla = bla;
        }

        @Override
        public boolean test(String o) {
            return o.equals(bla);
        }
    }
}
