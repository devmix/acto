# Use Cases

Runnable, tested examples of typical patterns built on top of the dispatcher. Every use case
has a `main` class (a demo) and a Spock spec (an executable proof).

Run all examples' tests:

```bash
./mvnw -ntp verify -pl examples
```

Run a single demo, e.g. the ledger:

```bash
./mvnw -ntp compile exec:java -pl examples -Dexec.mainClass=com.github.devmix.process.acto.examples.ledger.LedgerExample
```

| # | Use case | Pattern | Demo |
|---|----------|---------|------|
| 1 | [Transaction ledger](src/main/java/com/github/devmix/process/acto/examples/ledger) | accepted work is never lost: `destroy(id, false)` drains the queue first | `LedgerExample` |
| 2 | [ETL pipeline](src/main/java/com/github/devmix/process/acto/examples/etl) | `DEPENDS_ON` wiring: source → transformer → target, ordered start, graceful shutdown | `EtlExample` |
| 3 | [File writer holder](src/main/java/com/github/devmix/process/acto/examples/holder) | a non-thread-safe resource behind an object: thread safety without `synchronized` | `HolderExample` |
| 4 | [Rate limiter](src/main/java/com/github/devmix/process/acto/examples/limiter) | the mailbox is the mutex: sequential code instead of CAS/locks | `LimiterExample` |
| 5 | [Event bridge](src/main/java/com/github/devmix/process/acto/examples/bridge) | callback API → futures: `requestAndForget` + `CompletableFuture` | `BridgeExample` |
| 6 | [Write-through cache](src/main/java/com/github/devmix/process/acto/examples/cache) | concurrent readers, single-threaded writes, idle invalidation via `OnIdleListener` | `CacheExample` |
