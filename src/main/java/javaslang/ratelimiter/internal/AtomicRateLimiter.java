package javaslang.ratelimiter.internal;

import static java.lang.Long.min;
import static java.lang.System.nanoTime;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.locks.LockSupport.parkNanos;

import javaslang.ratelimiter.RateLimiter;
import javaslang.ratelimiter.RateLimiterConfig;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link AtomicRateLimiter} splits all nanoseconds from the start of epoch into cycles.
 * <p>Each cycle has duration of {@link RateLimiterConfig#limitRefreshPeriod} in nanoseconds.
 * <p>
 * <p>By contract on start of each cycle {@link AtomicRateLimiter} should
 * set {@link State#activePermissions} to {@link RateLimiterConfig#limitForPeriod}.
 * For the {@link AtomicRateLimiter} callers it is really looks so, but under the hood there is
 * some optimisations that will skip this refresh if {@link AtomicRateLimiter} is not used actively.
 * <p>
 * <p>All {@link AtomicRateLimiter} updates are atomic and state is encapsulated in {@link AtomicReference} to
 * {@link AtomicRateLimiter.State}
 */
public class AtomicRateLimiter implements RateLimiter {

    private final String name;
    private final RateLimiterConfig rateLimiterConfig;
    private final long cyclePeriodInNanos;
    private final int permissionsPerCycle;
    private final AtomicInteger waitingThreads;
    public final AtomicReference<State> state;


    public AtomicRateLimiter(String name, RateLimiterConfig rateLimiterConfig) {
        this.name = name;
        this.rateLimiterConfig = rateLimiterConfig;

        cyclePeriodInNanos = rateLimiterConfig.getLimitRefreshPeriod().toNanos();
        permissionsPerCycle = rateLimiterConfig.getLimitForPeriod();

        waitingThreads = new AtomicInteger(0);
        state = new AtomicReference<>(new State(0, 0, 0));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getPermission(final Duration timeoutDuration) {
        long timeoutInNanos = timeoutDuration.toNanos();
        State modifiedState = state.updateAndGet(
            activeState -> calculateNextState(timeoutInNanos, activeState)
        );
        return waitForPermissionIfNecessary(timeoutInNanos, modifiedState.nanosToWait);
    }

    /**
     * A side-effect-free function that can calculate next {@link State} from current.
     * It determines time duration that you should wait for permission and reserves it for you,
     * if you'll be able to wait long enough.
     *
     * @param timeoutInNanos max time that caller can wait for permission in nanoseconds
     * @param activeState    current state of {@link AtomicRateLimiter}
     * @return next {@link State}
     */
    public State calculateNextState(final long timeoutInNanos, final State activeState) {
        long currentNanos = currentNanoTime();
        long currentCycle = currentNanos / cyclePeriodInNanos;

        long nextCycle = activeState.activeCycle;
        int nextPermissions = activeState.activePermissions;
        if (nextCycle != currentCycle) {
            long elapsedCycles = currentCycle - nextCycle;
            long accumulatedPermissions = elapsedCycles * permissionsPerCycle;
            nextCycle = currentCycle;
            nextPermissions = (int) min(nextPermissions + accumulatedPermissions, permissionsPerCycle);
        }
        long nextNanosToWait = nanosToWaitForPermission(nextPermissions, currentNanos, currentCycle);
        State nextState = reservePermissions(timeoutInNanos, nextCycle, nextPermissions, nextNanosToWait);
        return nextState;
    }


    /**
     * Calculates time to wait for next permission as
     * [time to the next cycle] + [duration of full cycles until reserved permissions expire]
     *
     * @param availablePermissions currently available permissions, can be negative if some permissions have been reserved
     * @param currentNanos         current time in nanoseconds
     * @param currentCycle         current {@link AtomicRateLimiter} cycle
     * @return nanoseconds to wait for the next permission
     */
    public long nanosToWaitForPermission(final int availablePermissions, final long currentNanos, final long currentCycle) {
        if (availablePermissions > 0) {
            return 0L;
        } else {
            long nextCycleTimeInNanos = (currentCycle + 1) * cyclePeriodInNanos;
            long nanosToNextCycle = nextCycleTimeInNanos - currentNanos;
            int fullCyclesToWait = (-availablePermissions) / permissionsPerCycle;
            return (fullCyclesToWait * cyclePeriodInNanos) + nanosToNextCycle;
        }
    }

    /**
     * Determines whether caller can acquire permission before timeout or not and then creates corresponding {@link State}.
     * Reserves permissions only if caller can successfully wait for permission.
     *
     * @param timeoutInNanos max time that caller can wait for permission in nanoseconds
     * @param cycle          cycle for new {@link State}
     * @param permissions    permissions for new {@link State}
     * @param nanosToWait    nanoseconds to wait for the next permission
     * @return new {@link State} with possibly reserved permissions and time to wait
     */
    public State reservePermissions(final long timeoutInNanos, final long cycle, final int permissions, final long nanosToWait) {
        boolean canAcquireInTime = timeoutInNanos >= nanosToWait;
        int permissionsWithReservation = permissions;
        if (canAcquireInTime) {
            permissionsWithReservation--;
        }
        return new State(cycle, permissionsWithReservation, nanosToWait);
    }

    /**
     * If nanosToWait is bigger than 0 it tries to park {@link Thread} for nanosToWait but not longer then timeoutInNanos.
     *
     * @param timeoutInNanos max time that caller can wait
     * @param nanosToWait    nanoseconds caller need to wait
     * @return true if caller was able to wait for nanosToWait without {@link Thread#interrupt} and not exceed timeout
     */
    public boolean waitForPermissionIfNecessary(final long timeoutInNanos, final long nanosToWait) {
        boolean canAcquireImmediately = nanosToWait <= 0;
        boolean canAcquireInTime = timeoutInNanos >= nanosToWait;

        if (canAcquireImmediately) {
            return true;
        }
        if (canAcquireInTime) {
            return waitForPermission(nanosToWait);
        }
        waitForPermission(timeoutInNanos);
        return false;
    }

    /**
     * Parks {@link Thread} for nanosToWait.
     * <p>If the current thread is {@linkplain Thread#interrupted}
     * while waiting for a permit then it won't throw {@linkplain InterruptedException},
     * but its interrupt status will be set.
     *
     * @param nanosToWait nanoseconds caller need to wait
     * @return true if caller was not {@link Thread#interrupted} while waiting
     */
    public boolean waitForPermission(final long nanosToWait) {
        waitingThreads.incrementAndGet();
        long deadline = currentNanoTime() + nanosToWait;
        boolean wasInterrupted = false;
        while (currentNanoTime() < deadline && !wasInterrupted) {
            long sleepBlockDuration = deadline - currentNanoTime();
            parkNanos(sleepBlockDuration);
            wasInterrupted = Thread.interrupted();
        }
        waitingThreads.decrementAndGet();
        if (wasInterrupted) {
            currentThread().interrupt();
        }
        return !wasInterrupted;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RateLimiterConfig getRateLimiterConfig() {
        return rateLimiterConfig;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AtomicRateLimiterMetrics getMetrics() {
        return new AtomicRateLimiterMetrics();
    }

    /**
     * <p>{@link AtomicRateLimiter.State} represents immutable state of {@link AtomicRateLimiter} where:
     * <ul>
     * <li>activeCycle - {@link AtomicRateLimiter} cycle number that was used
     * by the last {@link AtomicRateLimiter#getPermission(Duration)} call.</li>
     * <p>
     * <li>activePermissions - count of available permissions after
     * last the last {@link AtomicRateLimiter#getPermission(Duration)} call.
     * Can be negative if some permissions where reserved.</li>
     * <p>
     * <li>nanosToWait - count of nanoseconds to wait for permission for
     * the last {@link AtomicRateLimiter#getPermission(Duration)} call.</li>
     * </ul>
     */
    public static class State {

        private final long activeCycle;
        private final int activePermissions;
        private final long nanosToWait;

        public State(final long activeCycle, final int activePermissions, final long nanosToWait) {
            this.activeCycle = activeCycle;
            this.activePermissions = activePermissions;
            this.nanosToWait = nanosToWait;
        }

    }

    /**
     * Enhanced {@link Metrics} with some implementation specific details
     */
    public final class AtomicRateLimiterMetrics implements Metrics {

        private AtomicRateLimiterMetrics() {
        }

        /**
         * {@inheritDoc}
         *
         * @return
         */
        @Override
        public int getNumberOfWaitingThreads() {
            return waitingThreads.get();
        }

        /**
         * @return estimated time duration in nanos to wait for the next permission
         */
        public long getNanosToWait() {
            State currentState = state.get();
            State estimatedState = calculateNextState(-1, currentState);
            return estimatedState.nanosToWait;
        }

        /**
         * Estimates count of permissions available permissions.
         * Can be negative if some permissions where reserved.
         *
         * @return estimated count of permissions
         */
        public long getAvailablePermissions() {
            State currentState = state.get();
            State estimatedState = calculateNextState(-1, currentState);
            return estimatedState.activePermissions;
        }
    }

    /**
     * Created only for test purposes. Simply calls {@link System#nanoTime()}
     */
    private long currentNanoTime() {
        return nanoTime();
    }
}
