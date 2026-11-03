# Benchmarks

JMH benchmarks live in the `benchmarks` module (not part of the default build):

```bash
./mvnw -ntp -Pbenchmarks -pl benchmarks clean verify
java -jar benchmarks/target/benchmarks.jar
```

Run them manually (or via the `benchmarks.yml` workflow-dispatch), not on every PR.
Commit a new `baseline-<date>.json` + table here when behavior changes materially.

## Baseline 2026-11-03

Environment: JDK 21.0.6-graal, Linux x86_64, 16 cores, JMH 1.37, single thread.

> Note: this baseline was produced with GraalVM 21.0.6 (the local JDK), while the CI
> workflow runs Temurin 21 — CI numbers are comparable in trend, not bit-for-bit.

| Benchmark | Mode | Score | Error | Unit |
|-----------|------|-------|-------|------|
| `requestAndForget` | thrpt | 1.631 | ±0.661 | ops/µs |
| `requestFireAndForget` | thrpt | 1.999 | ±2.168 | ops/µs |
| `requestRoundTrip` | thrpt | 0.273 | ±0.011 | ops/µs |
| `requestRoundTrip` | avgt | 3.725 | ±0.179 | µs/op |
| `gracefulDestroyEmptyQueue` | ss | 66.727 | ±19.643 | µs/op |
| `forcedDestroyEmptyQueue` | ss | 81.135 | ±31.278 | µs/op |
| `forcedDestroyLoadedQueue` (100 × 1 ms) | ss | 1019.330 | ±410.132 | µs/op |
| `gracefulDestroyLoadedQueue` (100 × 1 ms) | ss | 109736.433 | ±1012.310 | µs/op |
| `plainWrite` (100 writes) | avgt | 0.488 | ±0.015 | ns/op |
| `volatileWrite` (100 writes) | avgt | 477.191 | ±8.635 | ns/op |

Conclusions:

- Fire-and-forget dispatch sustains ~1.6–2.0 M msg/s, confirming the ~2 M msg/s figure
  from the stress-test discussions.
- A full `request().get()` round-trip costs ~3.7 µs (~273k round-trips/s).
- Graceful destroy of a loaded queue pays exactly the queued work (100 × 1 ms sleeps);
  forced destroy discards it and returns in ~1 ms.
- A volatile write costs ~4.8 ns (477 ns / 100) vs ~0.005 ns for a plain field: the
  store-load barrier is real but negligible against the ~3.7 µs dispatch round-trip,
  which settles the PR #9 discussion in favor of keeping the fields volatile.
