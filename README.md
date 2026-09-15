# Banking Transaction Processor

A timeboxed kata: a service that processes deposits, withdrawals, and transfers
for multiple accounts, keeps a timestamped ledger per account, and exposes query
APIs for balances and histories.

The work is intentionally incomplete relative to a production bank. The goal was
to leave a small, compilable core whose behaviour is documented by tests.

## How to run

```bash
mvn test
```

Requires JDK 17+ and Maven.

Public API: `com.banking.application.BankingService`.

```java
BankingService banking = new BankingService(
        new InMemoryAccountRepository(),
        Clock.systemUTC()
);

banking.openAccount(new AccountId("alice"), Money.of("100.00"));
banking.deposit(new AccountId("alice"), Money.of("20.00"));
banking.withdraw(new AccountId("alice"), Money.of("5.00"));
banking.transfer(new AccountId("alice"), new AccountId("bob"), Money.of("10.00"));

Money balance = banking.balanceOf(new AccountId("alice"));
List<Transaction> history = banking.historyOf(new AccountId("alice"));
```

## Understanding

The problem is not "move numbers around". It is "protect a money invariant while
leaving an auditable trail".

Invariants treated as non-negotiable:

- An account has a unique id and a non-negative balance.
- Operation amounts must be strictly positive and at most two decimal places.
- Withdrawals and transfers must not overdraft. A rejected operation leaves
  balances and ledgers unchanged.
- Every successful mutation appends a ledger entry with a timestamp and the
  resulting balance.
- A transfer is one business operation with two ledger lines (out on the source,
  in on the destination) sharing the same timestamp.

Edge cases encoded in tests:

- zero and negative amounts
- more than two decimal places (`1.001`)
- missing / blank account ids
- duplicate account ids
- unknown accounts on query, deposit, and transfer
- transfer to the same account
- overdraft on withdraw and transfer
- opening with a zero balance (no ledger noise) vs a funded opening (recorded
  as a deposit)
- callers cannot mutate the returned history list

## Design

SOLID was used as a constraint on size, not as a reason to add types.

| Principle | Choice |
| --- | --- |
| SRP | `Money` owns amount rules. `Account` owns balance + ledger. `BankingService` owns multi-account use cases. |
| OCP | Persistence sits behind `AccountRepository`. Time sits behind `java.time.Clock`. |
| LSP | `InMemoryAccountRepository` is a complete implementation of the port. |
| ISP | The repository is three methods. There is no "god" service interface. |
| DIP | The application layer depends on `AccountRepository` and `Clock`, not on a database or `Instant.now()`. |

Object model:

```
BankingService  -->  AccountRepository
       |                    ^
       v                    |
    Account  (aggregate)    InMemoryAccountRepository
       |
       +-- Money
       +-- List<Transaction>
```

`Account.transferTo` mutates both aggregates in one method so a failed debit
never credits the destination. Same-account transfer is rejected in the service
because that is a use-case rule ("who may pay whom"), not a balance rule.

`Clock` is injected so history tests can assert timestamps without sleeping or
stubbing static time.

## Trade-offs

**Java API, not HTTP.** The letter asks for APIs to query balances and histories.
I read that as a stable application contract. A REST layer would have consumed
the timebox without changing the banking rules. With more time I would add a
thin HTTP adapter over `BankingService`, not invert the design around controllers.

**Single implied currency.** Multi-currency would need FX, rounding policy, and
a currency on every `Money`. That is a different product. `Money` rejects extra
fractional digits instead of rounding so we never silently change a customer's
amount.

**In-memory store.** Enough to prove the domain. No durability, no true
transaction isolation. Transfers save both accounts after both mutations; if we
later use a database, that save must become one transaction.

**Mutable aggregates.** An immutable `Account` that returns a new instance per
operation is nicer for reasoning, but the in-memory repository would then have
to replace the stored instance on every call. Mutable + encapsulated is the
smaller design for this kata.

**No concurrency control.** Parallel transfers on the same pair of accounts can
race. I did not add account-level locks because a correct lock order is easy to
get wrong and would look more sophisticated than the rest of the design. A
database with row locks is the honest next step.

**Unchecked domain exceptions.** They keep the service API readable. Callers
can catch `BankingException` for any business failure.

## Iteration

1. Started with `Money` — if amounts are wrong, every operation is wrong.
2. Drove `Account` from tests: deposit, withdraw, overdraft, transfer, ledger.
3. Lifted multi-account rules into `BankingService` (open, lookup, same-account
   transfer, persistence).
4. Tightened `Money` after writing `1.001` as a test: reject extra scale rather
   than round.

What I would do with more time:

- persist with a transactional repository and unique account-id constraint
- account-level locking or optimistic versions for concurrent transfers
- pagination and date filters on history
- HTTP + idempotency keys for retried transfers
- statement/reporting views separate from the write model

## What I skipped, and why

- Spring Boot, Docker, and a database — ceremony, not behaviour.
- Authentication, limits, fees, holds, and interest — out of scope.
- Generated account ids — caller-supplied ids make the uniqueness rule obvious
  in tests. Generation can wrap `openAccount` later.
