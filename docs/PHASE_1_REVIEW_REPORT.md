# LedgerCore — Phase 1 Review Report (For External / ChatGPT Audit)

## 1. Phase 1 Goal & Scope
Phase 1 establishes the clean, minimal Spring Boot 4.2.0 / Java 21 foundation for **LedgerCore**:
- Versioned Flyway database schema (`V1__init.sql`) with `ddl-auto=validate`
- Multi-currency minor-unit precision (`Currency` enum: `INR(2)`, `USD(2)`, `JPY(0)`, `BHD(3)`)
- Account creation & lookup APIs (`POST /api/v1/accounts`, `GET /api/v1/accounts/{id}`)
- Database-level `CHECK` constraint preventing negative balances on `USER` accounts while permitting contra/issuance balances on `TREASURY` accounts
- Centralized exception handling (`@RestControllerAdvice`), Jakarta Bean Validation, and Spring Boot Actuator (`/actuator/health`, `/actuator/metrics`)

---

## 2. Files Created / Modified (`D:\ledgercore`)

| File | Responsibility |
|---|---|
| `pom.xml` | Added `spring-boot-starter-flyway`, `spring-boot-starter-actuator`, `postgresql` runtime driver |
| `src/main/resources/application.properties` | H2 (`MODE=PostgreSQL`), `ddl-auto=validate`, `open-in-view=false`, Flyway enabled, Actuator exposed |
| `src/main/resources/db/migration/V1__init.sql` | `accounts` table, SQL `CHECK` constraints, index on `(account_type, currency)`, 4 seeded `SYSTEM_TREASURY_*` rows |
| `src/main/java/com/ledgercore/domain/Currency.java` | Enum `INR(2)`, `USD(2)`, `JPY(0)`, `BHD(3)` with `minorUnits()` |
| `src/main/java/com/ledgercore/domain/AccountType.java` | Enum `USER`, `TREASURY` |
| `src/main/java/com/ledgercore/domain/AccountStatus.java` | Enum `ACTIVE`, `FROZEN` |
| `src/main/java/com/ledgercore/domain/Account.java` | Minimal JPA entity (`UUID`, `ownerName`, `currency`, `accountType`, `status`, `availableBalance`, `heldBalance`, `createdAt`) |
| `src/main/java/com/ledgercore/repository/AccountRepository.java` | `JpaRepository<Account, UUID>` |
| `src/main/java/com/ledgercore/dto/AccountDtos.java` | Java 21 `record`s (`CreateAccountRequest`, `AccountResponse`) with Bean Validation |
| `src/main/java/com/ledgercore/exception/AccountNotFoundException.java` | Domain 404 exception |
| `src/main/java/com/ledgercore/exception/GlobalExceptionHandler.java` | `@RestControllerAdvice` mapping `404`, `400 VALIDATION_ERROR`, `400 INVALID_REQUEST` |
| `src/main/java/com/ledgercore/service/AccountService.java` | `@Transactional` create and `readOnly = true` fetch |
| `src/main/java/com/ledgercore/controller/AccountController.java` | `POST /api/v1/accounts` (`201`) & `GET /api/v1/accounts/{id}` (`200`) |
| `src/test/java/com/ledgercore/AccountControllerTest.java` | 7 integration tests covering API, Flyway seeds, validation, DB constraint, Actuator |

---

## 3. Database Schema (`V1__init.sql`)
```sql
CREATE TABLE accounts (
    id                UUID PRIMARY KEY,
    owner_name        VARCHAR(120) NOT NULL,
    currency          VARCHAR(3)   NOT NULL,
    account_type      VARCHAR(16)  NOT NULL,
    status            VARCHAR(16)  NOT NULL,
    available_balance BIGINT       NOT NULL DEFAULT 0,
    held_balance      BIGINT       NOT NULL DEFAULT 0,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT chk_account_type CHECK (account_type IN ('USER', 'TREASURY')),
    CONSTRAINT chk_account_status CHECK (status IN ('ACTIVE', 'FROZEN')),
    CONSTRAINT chk_account_currency CHECK (currency IN ('INR', 'USD', 'JPY', 'BHD')),
    CONSTRAINT chk_non_negative_user_balance CHECK (
        account_type = 'TREASURY' OR (available_balance >= 0 AND held_balance >= 0)
    )
);
```
*Design choice:* New `USER` accounts always open at `0` balance; deposits will be implemented in Phase 2 as double-entry transfers from `SYSTEM_TREASURY_<CURRENCY>` so every rupee/cent is backed by an immutable ledger entry from Day 1.

---

## 4. Test Verification Results (`.\mvnw.cmd test`)
- **Total:** `8` tests run (`7` in `AccountControllerTest` + `1` in `LedgercoreApplicationTests`)
- **Passed:** `8` | **Failures:** `0` | **Errors:** `0` | **Skipped:** `0`
- **Verified Behaviors:**
  1. Flyway runs cleanly and seeds all 4 `TREASURY` accounts (`INR`, `USD`, `JPY`, `BHD`).
  2. `POST /api/v1/accounts` creates an `ACTIVE` `USER` account at `0` balance (`201 Created`) and `GET /api/v1/accounts/{id}` retrieves it (`200 OK`).
  3. `JPY` returns `minorUnits: 0` and `BHD` returns `minorUnits: 3`.
  4. Blank `ownerName` returns `400 VALIDATION_ERROR` with field details; invalid currency `"XYZ"` returns `400 INVALID_REQUEST`.
  5. Unknown UUID returns `404 ACCOUNT_NOT_FOUND`.
  6. Direct SQL `INSERT` of a negative `available_balance` for a `USER` account throws `DataIntegrityViolationException` (`chk_non_negative_user_balance`).
  7. `/actuator/health` returns `200 OK` (`{"status":"UP"}`).

---

## 5. Known Limitations & Intentional Deferrals (For Next Phases)
1. **No Deposits / Transfers Yet:** Intentional—deferred to Phase 2 so deposits flow through `transfers` + `ledger_entries` from `SYSTEM_TREASURY`.
2. **H2 `MODE=PostgreSQL` Locally:** Docker is not installed on this machine yet; PostgreSQL driver is included and `docker-compose.yml` + Testcontainers Postgres suite will be added once Docker is available.
3. **No Authentication / Rate Limiting Yet:** Planned as lightweight API-key guard in Phase 7.
4. **Questions for Reviewer:**
   - Are there any subtle issues with the `V1__init.sql` constraints or `Account` entity design before Phase 2 adds `transfers` and `ledger_entries`?
   - Should `accounts` carry a `@Version` column or `updated_at` timestamp now, or is `PESSIMISTIC_WRITE` + `ledger_entries` sufficient?
