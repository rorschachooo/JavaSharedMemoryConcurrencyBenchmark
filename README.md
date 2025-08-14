# JavaSharedMemoryConcurrencyBenchmark

CPU-bound shared-memory synchronization benchmarks using the sum of Euler’s totient over `[1..upper]`.
We report two views: **Throughput (ops/s)** for small `upper` and **Runtime (ms/op)** for large `upper`.

## Environment (tested)
- OS: Windows 11 24H2 (WSL2: Ubuntu 24.04.2 LTS)
- JDK: OpenJDK 21.0.8+9 (HotSpot, x64)
- Build: Maven (JMH 1.37 via dependencies)

> Tip: verify toolchain with `java -version` and `mvn -v`. Using JDK 21 is recommended (JDK 17+ likely works but is not guaranteed).

## Build
```bash
mvn -q -DskipTests clean package
# Jar: target/JavaSharedMemoryConcurrencyBenchmark-1.0-SNAPSHOT.jar
```

## Run benchmarks

### A) Throughput (ops/s) — small `upper`
```bash
java -jar target/JavaSharedMemoryConcurrencyBenchmark-1.0-SNAPSHOT.jar '.*TotientSumBenchmark\.(testAtomicLong|testLongAdder|testReentrantLock|testSynchronized)$' -wi 5 -i 25 -f 2 -tu s -p upper=1000,5000,10000 -p threadCount=1,2,4,8,16,32 -rf csv -rff results_4way_f5_i8.csv -foe true -to 20m
```

### B) Runtime (ms/op) — large `upper`
> Only run the `_Avgt` methods; set worker threads via `-p threadCount=...`.

**upper = 50k**
```bash
java -jar target/JavaSharedMemoryConcurrencyBenchmark-1.0-SNAPSHOT.jar '.*TotientSumBenchmark.*_Avgt$' -wi 2 -w 1s -i 5 -r 1s -f 2 -tu ms -p upper=50000 -p threadCount=4,8,16,32 -rf csv -rff results_avgt_50k_formal.csv -foe true -to 20m
```

**T = 1 baseline for 50k (for speedup)**
```bash
java -jar target/JavaSharedMemoryConcurrencyBenchmark-1.0-SNAPSHOT.jar '.*TotientSumBenchmark.*_Avgt$' -wi 0 -i 1 -r 200ms -f 1 -tu ms -p upper=50000 -p threadCount=1 -rf csv -rff results_avgt_50k_T1_baseline.csv -foe true -to 5m
```

**upper = 100k**
```bash
java -jar target/JavaSharedMemoryConcurrencyBenchmark-1.0-SNAPSHOT.jar '.*TotientSumBenchmark.*_Avgt$' -wi 2 -w 1s -i 5 -r 2s -f 2 -tu ms -p upper=100000 -p threadCount=4,8,16,32 -rf csv -rff results_avgt_100k_formal.csv -foe true -to 30m
```

## Datasets (CSV, repo root)
- results_4way_f5_i8.csv
- results_avgt_50k_formal.csv
- results_avgt_50k_T1_baseline.csv
- results_avgt_100k_formal.csv

## Figures (PNG, under `figures/`)
- figures/runtime_50k_zoom.png
- figures/runtime_100k_zoom.png
- figures/speedup_50k_simple.png
- figures/speedup_100k_T4_simple.png
- figures/throughput_upper_1000.png
- figures/throughput_upper_5000.png
- figures/throughput_upper_10000.png

## Notes
- Control threads with `-p threadCount=...` (do **not** use `-t` in this design).
- Use `-foe true` and `-to` to avoid hangs on very slow combinations.
- For extremely large `upper` (e.g., 500k+), consider `-bm ss` (SingleShot).
