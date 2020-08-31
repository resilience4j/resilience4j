/*
 *
 *  Copyright 2020 Emmanouil Gkatziouras
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package com.google.common.util.concurrent;


import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.internal.RefillRateLimiter;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.TimeUnit;


/**
 * {@link SmoothBurstyDecorator} is a decorator class of the {@link SmoothRateLimiter.SmoothBursty} Rate Limiter.
 * Extra methods to support creation and re-configuration through RateLimiterConfig are applied.
 * The burst of permits cannot be un-set and the rate limiter needs to be reconfigured
 */
public class SmoothBurstyDecorator extends RateLimiter {

    private static final int NANOS_PER_MICRO = 1000;
    private static final long NANOS_IN_SECOND = 1_000_000_000l;

    private final SmoothRateLimiter.SmoothBursty smoothBursty;

    public static SmoothBurstyDecorator create(RateLimiterConfig rateLimiterConfig) {
        SleepingStopwatch stopwatch = SleepingStopwatch.createFromSystemTimer();
        BigDecimal permitsPerSecond = permissionsPerSecond(rateLimiterConfig);
        BigDecimal maxBurstSeconds = maxBurstSeconds(rateLimiterConfig, permitsPerSecond);
        return new SmoothBurstyDecorator(stopwatch, permitsPerSecond.doubleValue(), maxBurstSeconds.doubleValue());
    }

    private static final BigDecimal permissionsPerSecond(RateLimiterConfig rateLimiterConfig) {
        BigDecimal permissionsInPeriod = BigDecimal.valueOf(rateLimiterConfig.getLimitForPeriod());
        BigDecimal permissionsPeriodInNanos = BigDecimal.valueOf(rateLimiterConfig.getLimitRefreshPeriod().toNanos());

        BigDecimal mul = BigDecimal.valueOf(NANOS_IN_SECOND).multiply(permissionsInPeriod);
        return mul.divide(permissionsPeriodInNanos);
    }

    private static final BigDecimal maxBurstSeconds(RateLimiterConfig rateLimiterConfig, BigDecimal permitsPerSecond) {
        int burstLimit = rateLimiterConfig.getBurstLimit();
        return BigDecimal.valueOf(burstLimit).divide(permitsPerSecond);
    }


    public SmoothBurstyDecorator(SleepingStopwatch stopwatch, double permitsPerSecond, double maxBurstSeconds) {
        super(stopwatch);
        this.smoothBursty = new SmoothRateLimiter.SmoothBursty(stopwatch, maxBurstSeconds);
        smoothBursty.setRate(permitsPerSecond);
    }

    public void updateRate(RateLimiterConfig rateLimiterConfig) {
        BigDecimal permissionsPerSecond = permissionsPerSecond(rateLimiterConfig);
        smoothBursty.setRate(permissionsPerSecond.doubleValue());
    }

    /**
     * Fetch the available permissions
     * @return
     */
    public int getAvailablePermissions() {
        return (int) smoothBursty.storedPermits;
    }

    /**
     * Returns nanos
     * @param permits
     * @return
     */
    public long reservePermission(int permits) {
        long microsToWait = smoothBursty.reserve(permits);
        return microsToWait * NANOS_PER_MICRO;
    }

    /**
     * This is needed since it affects the {@link RateLimiter#setRate} method
     * @param permitsPerSecond
     * @param nowMicros
     */
    @Override
    void doSetRate(double permitsPerSecond, long nowMicros) {
        smoothBursty.setRate(permitsPerSecond);
    }

    /**
     * This is needed since it affects the {@link RateLimiter#getRate()} method
     * @return
     */
    @Override
    double doGetRate() {
        return smoothBursty.getRate();
    }

    @Override
    public double acquire() {
        return smoothBursty.acquire();
    }

    @Override
    public double acquire(int permits) {
        return smoothBursty.acquire(permits);
    }

    @Override
    public boolean tryAcquire(Duration timeout) {
        return smoothBursty.tryAcquire(timeout);
    }

    @Override
    public boolean tryAcquire(long timeout, TimeUnit unit) {
        return smoothBursty.tryAcquire(timeout, unit);
    }

    @Override
    public boolean tryAcquire(int permits) {
        return smoothBursty.tryAcquire(permits);
    }

    @Override
    public boolean tryAcquire() {
        return smoothBursty.tryAcquire();
    }

    @Override
    public boolean tryAcquire(int permits, Duration timeout) {
        return smoothBursty.tryAcquire(permits, timeout);
    }

    @Override
    public boolean tryAcquire(int permits, long timeout, TimeUnit unit) {
        return smoothBursty.tryAcquire(permits, timeout, unit);
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "SmoothBurstyDecorator[stableRate=%3.1fqps]", getRate());
    }

    @Override
    long queryEarliestAvailable(long nowMicros) {
        return smoothBursty.queryEarliestAvailable(nowMicros);
    }

    @Override
    long reserveEarliestAvailable(int permits, long nowMicros) {
        return smoothBursty.reserveEarliestAvailable(permits, nowMicros);
    }

}
