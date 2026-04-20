/*
 *
 *  Copyright 2024 Florentin Simion and Rares Vlasceanu
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
package io.github.resilience4j.core.metrics;

import io.github.resilience4j.core.CASBackoffUtil;
import io.github.resilience4j.core.Clock;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.TimeUnit;

/**
 * Implements a lock-free time sliding window using a single linked list, represented by a head and a tail reference.
 * The algorithm of the sliding window is very similar as for {@link LockFreeFixedSizeSlidingWindowMetrics}.
 * <p>
 * Each node of the sliding window represents the aggregated stats across 1 second (time slice). During a time slice,
 * the algorithm is very simple consisting of a classical CAS-loop, in which the stats are copied, incremented, and
 * a swap is attempted.
 * <p>
 * The complexity of the algorithm comes when the time slice is advanced. The algorithm needs to ensure that only
 * one thread succeeds in advancing the time slice, i.e. a single time slice is advanced not multiple. This is achieved
 * via an extra check when adding a new entry to the window, which guarantees that it is added only if it
 * respects the time order, i.e. ith time slice is added only if it is preceded by the (i-1)th time slice.
 * <p>
 * It also needs to ensure that no stats updates are lost by updating a time slice in the past. This is accounted via
 * a processed flag in the time slice, which marks that no further increments should happen for it.
 * <p>
 *
 */
public class LockFreeSlidingTimeWindowMetrics implements Metrics {
    private static final long TIME_SLICE_DURATION_IN_NANOS = TimeUnit.SECONDS.toNanos(1);

    private static final VarHandle HEAD;
    private static final VarHandle TAIL;

    private static final VarHandle TIME_SLICE;
    private static final VarHandle NEXT;

    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();

            HEAD = l.findVarHandle(LockFreeSlidingTimeWindowMetrics.class, "headRef", Node.class);
            TAIL = l.findVarHandle(LockFreeSlidingTimeWindowMetrics.class, "tailRef", Node.class);

            TIME_SLICE = l.findVarHandle(Node.class, "timeSlice", TimeSlice.class);
            NEXT = l.findVarHandle(Node.class, "next", Node.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final Clock clock;
    private final int windowSize;

    private volatile Node headRef;
    private volatile Node tailRef;

    public LockFreeSlidingTimeWindowMetrics(int windowSize, Clock clock) {
        long time = clock.monotonicTime();

        this.clock = clock;
        this.windowSize = windowSize;

        this.headRef = new Node(new TimeSlice(0, time, new PackedAggregation(), false), null);
        this.tailRef = headRef;

        for (int i = 1; i < this.windowSize; i++) {
            Node newNode = new Node(new TimeSlice(i, time, new PackedAggregation(), false), null);

            tailRef.next = newNode;
            tailRef = newNode;
        }
    }

    public LockFreeSlidingTimeWindowMetrics(int windowSize) {
        this(windowSize, Clock.SYSTEM);
    }

    @Override
    public Snapshot record(long duration, TimeUnit durationUnit, Outcome outcome) {
        int spinCount = 0;
        while (true) {
            advanceTimeSlice();

            Node tail = tailRef;
            TimeSlice current = tail.timeSlice;

            // The current time slice has been marked as processed, no further updates to it are allowed.
            if (current.processed) {
                // Apply backoff even for processed time slices to prevent busy-waiting
                spinCount = CASBackoffUtil.performBackoff(spinCount);
                continue;
            }

            TimeSlice next = current.copy();
            next.record(duration, durationUnit, outcome);

            // Updates stats in the current time slice.
            if (TIME_SLICE.compareAndSet(tail, current, next)) {
                return new SnapshotImpl(next.stats);
            }

            // Virtual thread and platform thread friendly backoff strategy
            spinCount = CASBackoffUtil.performBackoff(spinCount);
        }
    }

    @Override
    public Snapshot getSnapshot() {
        advanceTimeSlice();
        return new SnapshotImpl(tailRef.timeSlice.stats);
    }

    private void advanceTimeSlice() {
        int spinCount = 0;
        while (true) {
            Node tail = tailRef;
            TimeSlice current = tail.timeSlice;

            long now = clock.monotonicTime();
            long elapsedTime = now - current.time;

            if (elapsedTime < TIME_SLICE_DURATION_IN_NANOS) {
                return;
            }

            // This check is an optimization to avoid the below CAS.
            //
            // It can happen that the current time slice is already marked as processed by another thread, which
            // didn't manage it to actually advance the time slice.
            if (!current.processed) {
                TimeSlice processed = new TimeSlice(current.second, current.time, current.stats, true);

                // Mark the current time slice as processed so that no further updates are allowed.
                if (TIME_SLICE.compareAndSet(tail, current, processed)) {
                    current = processed;
                } else {
                    // Virtual thread and platform thread friendly backoff strategy
                    spinCount = CASBackoffUtil.performBackoff(spinCount);
                    continue;
                }
            }

            // The elapsed time slice can be larger than the window size, in which case we need to do
            // a full window advancement.
            long elapsedSlices = Math.min(elapsedTime / TIME_SLICE_DURATION_IN_NANOS, windowSize);
            long elapsedTimeInNextSlice = elapsedTime - (elapsedSlices * TIME_SLICE_DURATION_IN_NANOS);

            // This can happen when the elapsed time is larger than the time tracked by the window, so the time
            // slice will be relative to the current time. Basically, the next slice time will be the current time.
            if (elapsedTimeInNextSlice >= TIME_SLICE_DURATION_IN_NANOS) {
                elapsedTimeInNextSlice = 0;
            }

            int nextSecond = (current.second + 1) % windowSize;
            long nextTime = now - (elapsedSlices - 1) * TIME_SLICE_DURATION_IN_NANOS - elapsedTimeInNextSlice;

            // Tries to advance the window to the next time slice, it can fail, as another thread might have already
            // done so. This is ok, as what is important is that the window is advanced. And all threads will exit once
            // the window is advanced to the current time.
            updateWindow(nextSecond, nextTime);
        }
    }

    private void updateWindow(int second, long time) {
        int spinCount = 0;
        while (true) {
            Node head = headRef;
            Node headNext = head.next;

            Node tail = tailRef;
            Node tailNext = tail.next;

            if (head != headRef) {
                // Virtual thread and platform thread friendly backoff strategy
                spinCount = CASBackoffUtil.performBackoff(spinCount);
                continue;
            }

            if (tail != tailRef) {
                // Virtual thread and platform thread friendly backoff strategy
                spinCount = CASBackoffUtil.performBackoff(spinCount);
                continue;
            }

            TimeSlice headTimeSlice = head.timeSlice;
            TimeSlice tailTimeSlice = tail.timeSlice;

            int nextSecond = (tailTimeSlice.second + 1) % windowSize;

            // This guarantees that only one thread advances the time slice.
            // The time functions as a modification counter, helping us to avoid the ABA problem.
            if (second != nextSecond || time < tailTimeSlice.time) {
                return;
            }

            if (tailNext == null) {
                PackedAggregation nextStats = tailTimeSlice.stats.copy();
                nextStats.discard(headTimeSlice.stats);

                TimeSlice nextTimeSlice = new TimeSlice(nextSecond, time, nextStats, false);

                Node nextNode = new Node(nextTimeSlice, null);

                // We need to guarantee that the time slice has been moved, so no early exit is allowed if any of
                // the last - 1 CAS operations fail. Also, no operations that can spuriously fail are allowed.
                if (NEXT.compareAndSet(tail, null, nextNode)) {
                    if (HEAD.compareAndSet(this, head, headNext)) {
                        // If this fails, it means that the tail has been advanced by another thread,
                        // so we can still exit.
                        TAIL.compareAndSet(this, tail, nextNode);
                        return;
                    }
                }
            } else if (tailNext.timeSlice.second == headTimeSlice.second) {
                if (HEAD.compareAndSet(this, head, headNext)) {
                    TAIL.compareAndSet(this, tail, tailNext);
                }
            } else {
                TAIL.compareAndSet(this, tail, tailNext);
            }

            // CAS operations failed, apply backoff strategy
            spinCount = CASBackoffUtil.performBackoff(spinCount);
        }
    }

    public static class TimeSlice {
        final int second;
        final long time;
        final PackedAggregation stats;
        final boolean processed;

        public TimeSlice(int second, long time, PackedAggregation stats, boolean processed) {
            this.second = second;
            this.time = time;
            this.stats = stats;
            this.processed = processed;
        }

        public TimeSlice copy() {
            return new TimeSlice(second, time, stats.copy(), processed);
        }

        public void record(Long duration, TimeUnit durationUnit, Outcome outcome) {
            stats.record(duration, durationUnit, outcome);
        }
    }

    private static class Node {
        volatile TimeSlice timeSlice;
        volatile Node next;

        Node(TimeSlice timeSlice, Node next) {
            TIME_SLICE.set(this, timeSlice);
            NEXT.set(this, next);
        }
    }
}
