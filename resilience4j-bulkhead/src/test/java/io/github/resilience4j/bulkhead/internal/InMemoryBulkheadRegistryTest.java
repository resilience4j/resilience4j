package io.github.resilience4j.bulkhead.internal;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class InMemoryBulkheadRegistryTest {

    BulkheadRegistry registry;


    @Before
    public void setup() {
        this.registry = new InMemoryBulkheadRegistry();
    }

    @Test
    public void testCreate() {

        // when
        BulkheadRegistry reg = BulkheadRegistry.create();

        // then
        assertThat(reg).isNotNull();
        assertThat(reg.getAllBulkheads()).isEmpty();
    }

    @Test
    public void testGetBulkheadByName() {

        // when
        Bulkhead one = registry.bulkhead("one", 1);
        Bulkhead two = registry.bulkhead("two", 2);

        // then
        assertThat(one).isNotNull();
        assertThat(two).isNotNull();
        assertThat(one).isNotSameAs(two);
        assertThat(one.getConfiguredDepth()).isEqualTo(1);
        assertThat(two.getConfiguredDepth()).isEqualTo(2);
    }

    @Test
    public void testGetAllBulkheads() {

        // given
        registry.bulkhead("foo", 1);
        registry.bulkhead("bar", 1);
        registry.bulkhead("foo", 1);

        // when
        int count = registry.getAllBulkheads().size();

        // then
        assertThat(count).isEqualTo(2);
    }

}
