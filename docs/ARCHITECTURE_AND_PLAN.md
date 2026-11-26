# LedgerCore — FinTech Payment Ledger Master Architecture & 8-Phase Plan

## Engineering Rules (Zero AI Bloat)
- **Minimal, clean Java 21:** `record`s for DTOs, small single-purpose methods, zero redundant wrappers.
- **Money & Currency Safety:** Amounts in `long` minor units + `Currency` enum (`INR(2)`, `USD(2)`, `JPY(0)`, `BHD(3)`). Cross-currency direct transfers rejected.
- **Schema Discipline:** Versioned Flyway SQL migrations (`V1__`, `V2__`, ...), never `ddl-auto=update`.
- **Database Parity:** Local dev/CI runs on H2 `MODE=PostgreSQL` when Docker isn't running, PLUS `docker-compose.yml` + `@Tag("postgres")` Testcontainers suite for real PostgreSQL MVCC/lock verification.
- **Observability & Validation:** `spring-boot-starter-validation`, centralized `@RestControllerAdvice`, `spring-boot-starter-actuator` metrics/health.
- **Profile Isolation:** `UNSAFE` race-condition simulator strictly isolated behind `@Profile("chaos")`—never compiled/exposed in default or `prod` profiles.

---

## Complete FinTech Feature Set & Edge-Case Hardening (8 Phases)

| Phase | Feature | Hardened Edge Cases & FinTech Proof |
|---|---|---|
| **Phase 1** | **Accounts, Currency Precision & Genesis Funding** | `Currency(minorUnits)` enum, `availableBalance` + `heldBalance`, `SYSTEM_TREASURY` contra-accounts per currency, Flyway `V1__init.sql`, Bean Validation, `GlobalExceptionHandler`, Actuator |
| **Phase 2** | **Double-Entry Transfers + Ordered Locking + Chaos Lab** | `SELECT ... FOR UPDATE` in ascending `accountId` order, `@Profile("chaos")` `UNSAFE` vs `SAFE` toggle, 100-thread `CountDownLatch` stress test |
| **Phase 3** | **Concurrent Idempotency Keys + Payload Hash** | `UNIQUE` constraint + `IN_PROGRESS` lock row prevents same-millisecond duplicate race; SHA-256 payload mismatch returns `422` |
| **Phase 4** | **Two-Phase Escrow (`Authorize → Capture / Void`) & Splits** | Strict `HELD → CAPTURED / VOIDED / EXPIRED` state machine, `expires_at` auto-release, deterministic remainder handling on splits so `SUM == 0` never breaks |
| **Phase 5** | **Atomic Risk & Velocity Guardrails + Account Freeze** | Sliding-window count & daily-amount limits enforced **inside** the transaction *after* acquiring `PESSIMISTIC_WRITE` lock (zero TOCTOU bypass), `ACTIVE`/`FROZEN` states |
| **Phase 6** | **Immutable Refunds & Partial Reversals** | Compensating double-entry reversals without mutating/deleting original rows; enforces `SUM(refunds) <= originalAmount` under lock |
| **Phase 7** | **SHA-256 Hash Chain, HMAC Checkpoint, Time-Travel & Auth** | `prev_hash -> entry_hash` chain + external-secret **HMAC-SHA256 signed checkpoint** (mitigates full-DB rewrite threat), `?at=<UTC Instant>` balance reconstruction, lightweight API-key admin guard |
| **Phase 8** | **Transactional Outbox, HMAC Webhooks & Chaos/Benchmark UI** | Outbox table + `@Scheduled` exponential-backoff delivery, HMAC-SHA256 webhook headers, built-in merchant receiver, interactive Chaos & Benchmark Console (`static/index.html`) + ADRs |

---

## Clean Package Structure (`src/main/java/com/ledgercore/`)
```text
com.ledgercore/
├── LedgercoreApplication.java
├── config/          # ApiKeyAuthFilter (lightweight), Chaos toggle, Scheduler
├── domain/          # JPA Entities + Currency enum
├── repository/      # Spring Data JPA + @Lock(PESSIMISTIC_WRITE) queries
├── dto/             # Java 21 records with Jakarta Bean Validation
├── exception/       # Domain exceptions + GlobalExceptionHandler
├── service/         # Focused services (Account, Transfer, Hold, Risk, Refund, Audit, Outbox)
└── controller/      # Thin REST controllers (+ @Profile("chaos") ChaosController)
```
