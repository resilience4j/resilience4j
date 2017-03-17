package io.github.robwin.osgi;

import io.github.robwin.circuitbreaker.CircuitBreaker;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.UrlProvisionOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.util.PathUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import javax.inject.Inject;
import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class OsgiIntegrationTest {

    @Inject
    private BundleContext bundleContext;

    @Configuration
    public Option[] config() {
        return options(
                mavenBundle("io.javaslang", "javaslang", "2.0.5"),
                mavenBundle("io.reactivex.rxjava2", "rxjava", "2.0.6"),
                mavenBundle("org.slf4j",  "slf4j-api", "1.7.24"),
                mavenBundle("javax.cache",  "cache-api", "1.0.0"),
                mavenBundle("org.reactivestreams", "reactive-streams", "1.0.0"),
                projectBundle(),
                junitBundles());
    }
 
    @Test
    public void bundleShouldBeActive() {
        assertBundleActive("io.github.robwin.javaslang-circuitbreaker");
    }

    @Test
    public void circuitBreakerShouldResolve() {
        CircuitBreaker.ofDefaults("test");
    }

    @Test(expected = ClassNotFoundException.class)
    public void internalClassesShouldNotBeExported() throws ClassNotFoundException {
        Class.forName("io.github.robwin.circuitbreaker.internal.RingBitSet");
    }

    private UrlProvisionOption projectBundle() {
        final File dir = new File( PathUtils.getBaseDir() + "/build/libs/");
        final File[] files = dir.listFiles();
        return bundle("reference:file:" + files[0].getPath());
    }

    private void assertBundleActive(String symbolicName) {
        assertEquals("Expecting bundle to be active:" + symbolicName, Bundle.ACTIVE, getBundleState(symbolicName));
    }

    private boolean isFragment(Bundle b) {
        return b.getHeaders().get("Fragment-Host") != null;
    }

    private Bundle getBundle(String symbolicName) {
        return getBundle(bundleContext, symbolicName);
    }

    static Bundle getBundle(BundleContext bc, String symbolicName) {
        for(Bundle b : bc.getBundles()) {
            if(symbolicName.equals(b.getSymbolicName())) {
                return b;
            }
        }
        return null;
    }

    /** @return bundle state, UNINSTALLED if absent */
    private int getBundleState(String symbolicName) {
        return getBundleState(getBundle(symbolicName));
    }

    /** @return bundle state, UNINSTALLED if absent, ACTIVE  */
    private int getBundleState(Bundle b) {
        if(b == null) {
            return Bundle.UNINSTALLED;
        } else if(isFragment(b)) {
            return Bundle.ACTIVE;
        } else {
            return b.getState();
        }
    }

}