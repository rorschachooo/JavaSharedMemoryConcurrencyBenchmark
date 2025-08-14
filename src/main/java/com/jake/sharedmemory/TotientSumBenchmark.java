package com.jake.sharedmemory;

import org.openjdk.jmh.annotations.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantLock;

/**
 * JMH benchmark comparing four shared-memory synchronization mechanisms:
 * - synchronized (intrinsic locking)
 * - ReentrantLock (explicit mutual exclusion lock)
 * - AtomicLong (lock-free atomic operation via CAS)
 * - LongAdder (striped counters that reduce contention at high concurrency)
 *
 * The benchmark computes the sum of Euler's totient function in a given range [1, upper],
 * partitioned across a configurable number of worker threads (threadCount).
 *
 * NOTE: To keep results directly comparable with existing data, each benchmark method
 * manually spawns 'threadCount' Java threads and aggregates partial sums via a pluggable
 * Accumulator strategy. Returning the final sum avoids dead-code elimination.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 25)
@Fork(2)
@State(Scope.Benchmark)
public class TotientSumBenchmark {

    @Param({ "1000", "5000", "10000" })
    int upper;

    @Param({ "1", "2", "4", "8", "16", "32" })
    int threadCount;

    /**
     * Launch 'threadCount' workers that each compute totients for a chunk
     * and apply the provided Accumulator to combine partial sums.
     */
    private void runWorkers(Accumulator acc) {
        final int chunk = Math.max(upper / threadCount, 0);
        Thread[] threads = new Thread[threadCount];

        for (int t = 0; t < threadCount; t++) {
            final int start = t * chunk + 1;
            final int end = (t == threadCount - 1) ? upper : (start + chunk - 1);

            threads[t] = new Thread(() -> {
                long local = 0L;
                if (start <= end) {
                    for (int n = start; n <= end; n++) {
                        local += Totient.euler(n);
                    }
                }
                acc.add(local);
            });
        }

        for (Thread th : threads) th.start();
        for (Thread th : threads) {
            try {
                th.join();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // --- Variant 1: synchronized ---
    @Benchmark
    public long testSynchronized() {
        final Object lock = new Object();
        final long[] sum = new long[1];
        runWorkers(delta -> {
            synchronized (lock) {
                sum[0] += delta;
            }
        });
        return sum[0];
    }

    // --- Variant 2: ReentrantLock ---
    @Benchmark
    public long testReentrantLock() {
        final ReentrantLock lock = new ReentrantLock(); // non-fair
        final long[] sum = new long[1];
        runWorkers(delta -> {
            lock.lock();
            try {
                sum[0] += delta;
            } finally {
                lock.unlock();
            }
        });
        return sum[0];
    }

    // --- Variant 3: AtomicLong ---
    @Benchmark
    public long testAtomicLong() {
        final AtomicLong sum = new AtomicLong(0L);
        runWorkers(sum::addAndGet);
        return sum.get();
    }

    // --- Variant 4: LongAdder ---
    @Benchmark
    public long testLongAdder() {
        final LongAdder adder = new LongAdder();
        runWorkers(adder::add);
        return adder.sum();
    }

    // Functional interface to pass accumulation strategy
    @FunctionalInterface
    private interface Accumulator {
        void add(long delta);
    }
    
    // ===== AverageTime (runtime, ms/op) variants for large `upper` =====
    // Method-level annotations override the class-level Throughput settings.
    // We reuse the same fields `upper` and `threadCount` (@Param) and the same runWorkers() helper.

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public long testSynchronized_Avgt() {
        final Object lock = new Object();
        final long[] sum = new long[1];
        runWorkers(delta -> {
            synchronized (lock) { sum[0] += delta; }
        });
        return sum[0];
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public long testReentrantLock_Avgt() {
        final ReentrantLock lock = new ReentrantLock(); // non-fair
        final long[] sum = new long[1];
        runWorkers(delta -> {
            lock.lock();
            try { sum[0] += delta; } finally { lock.unlock(); }
        });
        return sum[0];
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public long testAtomicLong_Avgt() {
        final AtomicLong sum = new AtomicLong(0L);
        runWorkers(sum::addAndGet);
        return sum.get();
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public long testLongAdder_Avgt() {
        final LongAdder adder = new LongAdder();
        runWorkers(adder::add);
        return adder.sum();
    }

}
