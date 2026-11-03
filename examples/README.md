# Use Cases

Runnable, tested examples of typical patterns built on top of the dispatcher. Every use case
has a Spock spec (an executable proof); the original six also have a `main` demo.

**Fit** — how well the problem fits the active object pattern (score out of 5): 5 means the
mailbox/idle/dependency features are the core of the solution, 3–3.5 means "works well, but a
plain concurrent structure could also do".

Run all examples' tests:

```bash
./mvnw -ntp verify -pl examples
```

Run a single demo, e.g. the ledger:

```bash
./mvnw -ntp compile exec:java -pl examples -Dexec.mainClass=com.github.devmix.process.acto.examples.ledger.LedgerExample
```

## Core patterns

| Use case | Fit | Pattern |
|----------|-----|---------|
| [Transaction ledger](src/main/java/com/github/devmix/process/acto/examples/ledger) | 4.5 | accepted work is never lost: `destroy(id, false)` drains the queue first |
| [ETL pipeline](src/main/java/com/github/devmix/process/acto/examples/etl) | 5 | `DEPENDS_ON` wiring: ordered start, graceful shutdown |
| [File writer holder](src/main/java/com/github/devmix/process/acto/examples/holder) | 4 | a non-thread-safe resource behind an object: thread safety without `synchronized` |
| [Rate limiter](src/main/java/com/github/devmix/process/acto/examples/limiter) | 4.5 | the mailbox is the mutex: sequential code instead of CAS/locks |
| [Event bridge](src/main/java/com/github/devmix/process/acto/examples/bridge) | 4 | callback API → futures: `requestAndForget` + `CompletableFuture` |
| [Write-through cache](src/main/java/com/github/devmix/process/acto/examples/cache) | 4 | concurrent readers, single-threaded writes, idle invalidation |

## ERP / ECM

| Use case | Fit | Pattern |
|----------|-----|---------|
| [Document checkout lock](src/main/java/com/github/devmix/process/acto/examples/erp/DocumentCheckoutObject.java) | 5 | one editor at a time; a forgotten checkout is auto-released by the idle heartbeat |
| [Invoice approval workflow](src/main/java/com/github/devmix/process/acto/examples/erp/InvoiceApprovalObject.java) | 4 | state machine: illegal transitions rejected in the mailbox |
| [Document number generator](src/main/java/com/github/devmix/process/acto/examples/erp/DocumentNumberObject.java) | 3.5 | gapless sequence; graceful destroy = no lost number |
| [Inventory reservation](src/main/java/com/github/devmix/process/acto/examples/erp/InventoryReservationObject.java) | 4.5 | reserve/release without oversell, no CAS |

## Fintech / HFT

| Use case | Fit | Pattern |
|----------|-----|---------|
| [Order book](src/main/java/com/github/devmix/process/acto/examples/fintech/OrderBookObject.java) | 5 | single-writer L2 book: lock-free `TreeMap` code, deterministic order |
| [Risk limit check](src/main/java/com/github/devmix/process/acto/examples/fintech/RiskLimitObject.java) | 4 | atomic check-and-update of exposure in one sequential step |
| [Idempotent event applier](src/main/java/com/github/devmix/process/acto/examples/fintech/IdempotentApplierObject.java) | 4.5 | exactly-once application of a redelivering feed by sequence number |
| [Circuit breaker / kill switch](src/main/java/com/github/devmix/process/acto/examples/fintech/CircuitBreakerObject.java) | 3.5 | the trip is ordered with respect to every in-flight request |
| [Payment idempotency](src/main/java/com/github/devmix/process/acto/examples/fintech/PaymentIdempotencyObject.java) | 4.5 | a retry storm with one key charges exactly once |

## Logistics

| Use case | Fit | Pattern |
|----------|-----|---------|
| [Shipment tracker](src/main/java/com/github/devmix/process/acto/examples/logistics/ShipmentTrackerObject.java) | 4 | out-of-order events buffered and applied strictly in sequence order |
| [Delivery slot allocator](src/main/java/com/github/devmix/process/acto/examples/logistics/SlotAllocatorObject.java) | 3.5 | per-slot capacity counters under concurrent booking |
| [Vehicle pool](src/main/java/com/github/devmix/process/acto/examples/logistics/VehiclePoolObject.java) | 4 | lease expiry reclaimed lazily on traffic — no timer thread |

## EdTech

| Use case | Fit | Pattern |
|----------|-----|---------|
| [Quiz session](src/main/java/com/github/devmix/process/acto/examples/edtech/QuizSessionObject.java) | 5 | the exam timer is the heartbeat: a silent student is auto-submitted on idle |
| [Progress journal](src/main/java/com/github/devmix/process/acto/examples/edtech/ProgressJournalObject.java) | 3.5 | append-only journal from many devices, replayable |
| [Enrollment cap](src/main/java/com/github/devmix/process/acto/examples/edtech/EnrollmentObject.java) | 3 | seat cap holds under an enrollment storm |

## MarTech

| Use case | Fit | Pattern |
|----------|-----|---------|
| [Campaign throttle](src/main/java/com/github/devmix/process/acto/examples/martech/CampaignThrottleObject.java) | 3.5 | per-campaign token buckets refilled lazily — no scheduler |
| [Lead dedup gateway](src/main/java/com/github/devmix/process/acto/examples/martech/LeadDedupObject.java) | 4 | a lead is forwarded once per TTL window under multi-channel races |

## MedTech

| Use case | Fit | Pattern |
|----------|-----|---------|
| [Patient access session](src/main/java/com/github/devmix/process/acto/examples/medtech/PatientAccessObject.java) | 4.5 | audit trail + auto-close when the clinician goes silent (idle) |
| [Device telemetry](src/main/java/com/github/devmix/process/acto/examples/medtech/DeviceTelemetryObject.java) | 4 | "device went silent" detection costs no polling thread |
| [Lab order state machine](src/main/java/com/github/devmix/process/acto/examples/medtech/LabOrderObject.java) | 3.5 | ordered transitions order → collected → in-lab → resulted |

## Observability

| Use case | Fit | Pattern |
|----------|-----|---------|
| [Log batcher](src/main/java/com/github/devmix/process/acto/examples/observability/LogBatcherObject.java) | 5 | flush on batch size or on the idle quiet period — heartbeat as the flush timer |
| [Alert state machine](src/main/java/com/github/devmix/process/acto/examples/observability/AlertStateMachineObject.java) | 4.5 | `for: 5m` semantics: pending becomes firing via the idle timeout |
| [Exporter quota](src/main/java/com/github/devmix/process/acto/examples/observability/ExporterQuotaObject.java) | 3.5 | a metric storm is capped per window with a visible dropped counter |

## ETL / Data processing

| Use case | Fit | Pattern |
|----------|-----|---------|
| [Checkpoint coordinator](src/main/java/com/github/devmix/process/acto/examples/data/CheckpointCoordinatorObject.java) | 5 | exactly-once sink: graceful destroy drains every accepted record |
| [DLQ retry](src/main/java/com/github/devmix/process/acto/examples/data/DlqRetryObject.java) | 3.5 | linear backoff retried lazily on traffic — no scheduler |
| [Schema registry](src/main/java/com/github/devmix/process/acto/examples/data/SchemaRegistryObject.java) | 3 | racing registrations conflict cleanly, versions advance by one |

## BI / Analytics

| Use case | Fit | Pattern |
|----------|-----|---------|
| [Session windowizer](src/main/java/com/github/devmix/process/acto/examples/bi/SessionWindowizerObject.java) | 5 | "session = N minutes of inactivity" with zero timers |
| [Rollup worker](src/main/java/com/github/devmix/process/acto/examples/bi/RollupWorkerObject.java) | 4 | downsampling driven by the heartbeat quiet period |
| [Incremental aggregator](src/main/java/com/github/devmix/process/acto/examples/bi/IncrementalAggregatorObject.java) | 3.5 | concurrent readers, single-threaded writes, exact quantiles |

## Misc

| Use case | Fit | Pattern |
|----------|-----|---------|
| [Saga coordinator](src/main/java/com/github/devmix/process/acto/examples/misc/SagaCoordinatorObject.java) | 4.5 | step results as messages, reverse-order compensation |
| [IoT device twin](src/main/java/com/github/devmix/process/acto/examples/misc/DeviceTwinObject.java) | 4 | merged reported state, offline detection by heartbeat |
| [Chat conversation](src/main/java/com/github/devmix/process/acto/examples/misc/ChatConversationObject.java) | 3.5 | gapless total order of messages under concurrent sends |
| [Player session](src/main/java/com/github/devmix/process/acto/examples/misc/PlayerSessionObject.java) | 3 | a click storm can never overspend the player |
