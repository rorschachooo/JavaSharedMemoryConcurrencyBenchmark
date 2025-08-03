package com.jake.sharedmemory;

import org.openjdk.jmh.annotations.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * JMH benchmark comparing two shared-memory concurrency models:
 * - synchronized (intrinsic locking)
 * - AtomicLong (lock-free atomic operation)
 *
 * The benchmark computes the sum of Euler's totient function in a given range,
 * with the range partitioned across multiple threads.
 */
@State(Scope.Benchmark)
public class TotientSumBenchmark {

    // Number of threads for parallel execution (JMH will test all values)
    @Param({"2", "4", "8", "16"})
    public int threadCount;

    // Upper bound of the range (inclusive) for totient sum
    @Param({"1000", "5000", "10000"})
    public int upper;

    /**
     * Benchmark using synchronized block to safely update a shared global sum.
     */
    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public long testSynchronized() throws InterruptedException {
        final Object lock = new Object();
        final long[] globalSum = {0};
        Thread[] threads = new Thread[threadCount];
        int chunk = upper / threadCount;

        // Create and start threads, each computing a segment of the range
        for (int t = 0; t < threadCount; t++) {
            int start = t * chunk + 1;
            int end = (t == threadCount - 1) ? upper : (start + chunk - 1);
            threads[t] = new Thread(() -> {
                long localSum = 0;
                for (int n = start; n <= end; n++) {
                    localSum += Totient.euler(n);
                }
                // Synchronized update of shared sum
                synchronized (lock) {
                    globalSum[0] += localSum;
                }
            });
        }

        // Launch all threads
        for (Thread thread : threads) thread.start();
        // Wait for all threads to finish
        for (Thread thread : threads) thread.join();

        return globalSum[0];
    }

    /**
     * Benchmark using AtomicLong to safely update a shared global sum.
     */
    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public long testAtomicLong() throws InterruptedException {
        final AtomicLong globalSum = new AtomicLong(0);
        Thread[] threads = new Thread[threadCount];
        int chunk = upper / threadCount;

        // Create and start threads, each computing a segment of the range
        for (int t = 0; t < threadCount; t++) {
            int start = t * chunk + 1;
            int end = (t == threadCount - 1) ? upper : (start + chunk - 1);
            threads[t] = new Thread(() -> {
                long localSum = 0;
                for (int n = start; n <= end; n++) {
                    localSum += Totient.euler(n);
                }
                // Atomic update of shared sum
                globalSum.addAndGet(localSum);
            });
        }

        // Launch all threads
        for (Thread thread : threads) thread.start();
        // Wait for all threads to finish
        for (Thread thread : threads) thread.join();

        return globalSum.get();
    }
    /**
     * High-contention version using synchronized block.
     * Each thread updates the shared global sum after every single totient calculation.
     */
    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public long testSynchronizedHighContention() throws InterruptedException {
        final Object lock = new Object();
        final long[] globalSum = {0};
        Thread[] threads = new Thread[threadCount];
        int chunk = upper / threadCount;

        for (int t = 0; t < threadCount; t++) {
            int start = t * chunk + 1;
            int end = (t == threadCount - 1) ? upper : (start + chunk - 1);
            threads[t] = new Thread(() -> {
                for (int n = start; n <= end; n++) {
                    long value = Totient.euler(n);
                    synchronized (lock) {
                        globalSum[0] += value;
                    }
                }
            });
        }
        for (Thread thread : threads) thread.start();
        for (Thread thread : threads) thread.join();
        return globalSum[0];
    }

    /**
     * High-contention version using AtomicLong.
     * Each thread updates the shared global sum after every single totient calculation.
     */
    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public long testAtomicLongHighContention() throws InterruptedException {
        final java.util.concurrent.atomic.AtomicLong globalSum = new java.util.concurrent.atomic.AtomicLong(0);
        Thread[] threads = new Thread[threadCount];
        int chunk = upper / threadCount;

        for (int t = 0; t < threadCount; t++) {
            int start = t * chunk + 1;
            int end = (t == threadCount - 1) ? upper : (start + chunk - 1);
            threads[t] = new Thread(() -> {
                for (int n = start; n <= end; n++) {
                    long value = Totient.euler(n);
                    globalSum.addAndGet(value);
                }
            });
        }
        for (Thread thread : threads) thread.start();
        for (Thread thread : threads) thread.join();
        return globalSum.get();
    }
}