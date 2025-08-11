# Java Shared-Memory Concurrency Benchmarks (JMH)

This repository contains reproducible microbenchmarks (JMH) comparing four synchronization mechanisms for a shared counter in Java:

- **synchronized** (intrinsic monitor)
- **ReentrantLock** (explicit mutex)
- **AtomicLong** (lock-free CAS)
- **LongAdder** (striped counter / sharded accumulator)

The workload is compute-bound: each worker thread computes Euler's totient values over a sub‑range and **aggregates locally** before a **single** update to the shared counter. This isolates synchronization overhead from business logic and makes results interpretable.

> Paper-friendly takeaway (from our latest run): LongAdder consistently outperforms AtomicLong once concurrency rises (≈5–16% gains depending on workload), while `ReentrantLock` matches or slightly exceeds LongAdder under this batched-aggregation pattern. `synchronized` scales the worst. As per‑operation work increases, differences narrow because synchronization becomes a smaller fraction of total time.

---

## Project layout
```
src/main/java/com/jake/sharedmemory/
  ├── Totient.java                  # GCD + Euler's totient
  └── TotientSumBenchmark.java      # @Benchmark methods for the four mechanisms
```

## Environment
- Java **JDK 17+** recommended
- Maven 3.8+
- Linux / macOS / WSL (results above collected on WSL)
- Run on AC power / performance mode to avoid down-clocking

## How it works (design)
- Parameter grid:
  - `upper ∈ {1000, 5000, 10000}` — per-thread compute workload
  - `threadCount ∈ {1, 2, 4, 8, 16, 32}` — number of worker threads **spawned inside the benchmark** (we do **not** use `-t`)
- Each benchmark method calls a shared `runWorkers(Accumulator)` that:
  1) partitions `[1..upper]` into `threadCount` chunks;
  2) spawns `threadCount` Java threads;
  3) each thread aggregates locally; and
  4) combines once via the provided mechanism (sync/lock/atomic/adder).

## Build
```bash
mvn -q -DskipTests clean package
```

If your build produces a fat-jar named like `target/JavaSharedMemoryConcurrencyBenchmark-1.0-SNAPSHOT.jar`, use it below. Otherwise, list the artifact to confirm:
```bash
ls target/*benchmarks*.jar || ls target/*.jar
```

## Run (recommended settings)
**Full sweep (4 methods × 3 uppers × 6 thread counts):**
```bash
java -jar target/JavaSharedMemoryConcurrencyBenchmark-1.0-SNAPSHOT.jar   '.*TotientSumBenchmark\.test(Synchronized|ReentrantLock|AtomicLong|LongAdder)$'   -bm thrpt -tu s -f 5 -wi 3 -i 8   -p upper=1000,5000,10000 -p threadCount=1,2,4,8,16,32   -rf csv -rff results_4way_f5_i8.csv
```
- `-bm thrpt` — throughput (ops/s)
- `-tu s` — seconds
- `-f 5` — JVM forks (fresh JVM per run; improves stability)
- `-wi 3`, `-i 8` — warmup/measurement iterations (balanced runtime vs. variance)
- We filter by regex to **only** run the four new methods.

## Output
JMH writes a CSV with columns like:
- `Benchmark` (`...testAtomicLong`, `...testLongAdder`, etc.)
- `Param: threadCount`, `Param: upper`
- `Score` (ops/s), `Score Error (99.9%)`
- `Threads` will show **1** because concurrency is created **inside** the benchmark.

Row count for the full sweep: `4 × 3 × 6 = 72` rows.

## Reproducibility notes
- Fix CPU governor / power mode, close heavy background tasks.
- Keep the same JDK, `-f/-wi/-i` values across runs for comparability.
- For key plots in a paper, consider re‑running those points with `-f 5 -i 12` to reduce variance further.

## Citation
If you use this code or results in publications, please cite this repository.

---

### Maintainer
- Zihui Jin
- GitHub: https://github.com/rorschachooo/JavaSharedMemoryConcurrencyBenchmark
