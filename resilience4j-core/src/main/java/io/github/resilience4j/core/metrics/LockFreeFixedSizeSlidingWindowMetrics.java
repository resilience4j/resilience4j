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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Implements a lock-free sliding window using a single linked list, represented by a head and a tail reference.
 * <p>
 * The idea is to initialize the list to the window size, where node N represents the stats for the N-th entry. Besides
 * the stats for the N-th entry, it also contains the stats for the whole window, which are carried over on each new
 * added entry.
 *
 * <p>
 * When a new record is added, the size of the window is maintained:
 * <ol>
 * <li>A new node is added at the tail of the list.</li>
 * <li>The head of the list is moved to the next node.</li>
 * <li>The tail of the list is moved to the next node.</li>
 * </ol>
 *
 * <p>
 * As these operations need to happen atomically, we use the VarHandle API to perform CAS operations on the head, tail
 * and next references. However, these updates are not atomically happening across these references, so we can end up
 * that each reference is updated by a different thread.
 *
 * <p>
 * The algorithm is based on the following conditions:
 * <ul>
 * <li>if there is no next node -> try to add one </li>
 * <li>else, if the head and tail represent the same N-th stats -> advance the head</li>
 * <li>else -> advance the tail</li>
 * </ul>
 *
 * <p>
 * All these operations are done via compare-and-swap operations.
 *
 */
public class LockFreeFixedSizeSlidingWindowMetrics implements Metrics {

    private static final VarHandle HEAD;
    private static final VarHandle TAIL;

    private static final VarHandle NEXT;

    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();

            HEAD = l.findVarHandle(LockFreeFixedSizeSlidingWindowMetrics.class, "headRef", Node.class);
            TAIL = l.findVarHandle(LockFreeFixedSizeSlidingWindowMetrics.class, "tailRef", Node.class);

            NEXT = l.findVarHandle(Node.class, "next", Node.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final int windowSize;

    private volatile Node tailRef;
    private volatile Node headRef;

    public LockFreeFixedSizeSlidingWindowMetrics(int windowSize) {
        this.windowSize = windowSize;

        this.headRef = new Node(0, new PackedAggregation(), null);
        this.tailRef = headRef;

        for (int i = 1; i < this.windowSize; i++) {
            Node newNode = new Node(i, new PackedAggregation(), null);

            tailRef.next = newNode;
            tailRef = newNode;
        }
    }

    @Override
    public Snapshot record(long duration, TimeUnit durationUnit, Outcome outcome) {
        int spinCount = 0;
        while (true) {
            Node head = headRef;
            Node headNext = head.next;

            Node tail = tailRef;
            Node tailNext = tail.next;

            // This check is necessary to make sure that the head has not fallen off the list.
            // This can happen when a thread reads the head, it is superseded from the CPU, and other threads
            // advances the head in the meantime.
            // Without this check we might end up adding into the queue a new node with incorrect stats.
            if (head != headRef) {
                // Virtual thread and platform thread friendly backoff strategy
                spinCount = CASBackoffUtil.performBackoff(spinCount);
                continue;
            }

            // This check is not actually needed for the well-functioning of the algorithm, it is just an optimization
            // to avoid unnecessary CAS operations, which are expensive.
            if (tail != tailRef) {
                // Virtual thread and platform thread friendly backoff strategy
                spinCount = CASBackoffUtil.performBackoff(spinCount);
                continue;
            }

            if (tailNext == null) {
                int nextId = (tail.id + 1) % windowSize;

                PackedAggregation nextStats = tail.stats.copy();

                nextStats.discard(head.stats);
                nextStats.record(duration, durationUnit, outcome);

                Node nextNode = new Node(nextId, nextStats, null);

                if (NEXT.compareAndSet(tail, null, nextNode)) {
                    // These operations can fail, as the head/tail might have been advanced by other threads,
                    // but we will exit anyway, so we can try to move the head/tail with some less-expensive operations.
                    //
                    // These operations can fail spuriously, but they are not critical, as the next thread will
                    // complete the job, by moving them.
                    if (HEAD.weakCompareAndSet(this, head, headNext)) {
                        TAIL.weakCompareAndSet(this, tail, nextNode);
                    }

                    return new SnapshotImpl(nextNode.stats);
                }
            } else if (tailNext.id == head.id) {
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

    @Override
    public Snapshot getSnapshot() {
        Node tail = tailRef;
        Node tailNext = tail.next;

        // As the tail can lag behind, the next pointer may be the actual tail.
        return new SnapshotImpl(Objects.requireNonNullElse(tailNext, tail).stats);
    }

    private static class Node {
        int id;
        PackedAggregation stats;
        volatile Node next;

        Node(int id, PackedAggregation stats, Node next) {
            this.id = id;
            this.stats = stats;
            NEXT.set(this, next);
        }
    }
}
