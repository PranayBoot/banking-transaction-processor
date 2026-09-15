# Test scenarios

These are the behaviours the kata promised. Each row is an executable test.
Run the catalogue with `./gradlew test`.

## Kata requirements

| Requirement | How it is validated |
| --- | --- |
| Accounts with unique IDs and balances | Open account; duplicate id is rejected |
| Deposit | Balance increases; ledger records `DEPOSIT` |
| Withdrawal | Balance decreases; overdraft is rejected |
| Transfer | Source decreases, destination increases; both ledgers are written |
| Prevent overdrafts | Withdraw/transfer above balance fail; state is unchanged |
| Prevent invalid amounts | Zero, negative, blank, non-numeric, and >2 decimal places fail |
| Ledger with timestamps | Every successful mutation has `occurredAt` |
| Query balance | `balanceOf` after mixed operations |
| Query history | Ordered, isolated per account |

---

## Banking service

### Open account

| Scenario | Expected |
| --- | --- |
| Open with unique id and funded balance | Balance equals opening amount |
| Open with zero balance | Balance is `0.00`, history is empty |
| Open with funded balance | History has one `DEPOSIT` at the current time |
| Open the same id twice | `DuplicateAccountException`; original balance kept |

### Deposit

| Scenario | Expected |
| --- | --- |
| Deposit to an existing account | Balance increases by the amount |
| Deposit is recorded | History has `DEPOSIT`, amount, timestamp; no counterparty |
| Deposit to a missing account | `AccountNotFoundException` |
| Deposit zero | `InvalidAmountException`; balance unchanged |
| Deposit `1.001` | `InvalidAmountException`; account not touched |

### Withdraw

| Scenario | Expected |
| --- | --- |
| Withdraw within balance | Balance decreases |
| Withdraw exact remaining balance | Balance becomes `0.00` |
| Withdraw more than balance | `InsufficientFundsException`; balance and history unchanged |
| Withdraw from a missing account | `AccountNotFoundException` |
| Withdraw zero | `InvalidAmountException` |

### Transfer

| Scenario | Expected |
| --- | --- |
| Transfer between two existing accounts | Source down, destination up |
| Transfer exact source balance | Source `0.00`, destination receives the full amount |
| Successful transfer ledger | Source `TRANSFER_OUT`, destination `TRANSFER_IN`, same timestamp, counterparties set |
| Missing destination | `AccountNotFoundException`; source unchanged |
| Missing source | `AccountNotFoundException`; destination unchanged |
| Transfer to the same account | `InvalidTransferException`; no extra ledger line |
| Transfer more than source balance | `InsufficientFundsException`; both accounts unchanged |
| Transfer zero | `InvalidAmountException`; both accounts unchanged |

### Query

| Scenario | Expected |
| --- | --- |
| Balance after open + deposit + withdraw | Latest balance |
| History after several operations | Types in chronological order |
| Two accounts | Each ledger contains only that account's entries |
| Balance or history of a missing account | `AccountNotFoundException` |
| Id with surrounding whitespace | Treated as the same account |

---

## Account (domain)

| Scenario | Expected |
| --- | --- |
| Open with zero | Empty ledger |
| Open with funds | Opening recorded as `DEPOSIT` with transaction id |
| Null opening balance | `InvalidAmountException` |
| Deposit | Balance and ledger updated |
| Deposit zero or null | `InvalidAmountException` |
| Withdraw available funds | Balance and `WITHDRAWAL` entry |
| Withdraw exact balance | Balance `0.00` |
| Overdraft withdraw | Exception, no extra ledger entry |
| Withdraw zero | `InvalidAmountException` |
| Transfer | Both ledgers, counterparties, shared time |
| Transfer without funds | Destination not credited |
| Transfer with null destination | `NullPointerException`; source unchanged |
| Mutate returned history | `UnsupportedOperationException` |

---

## Money

| Scenario | Expected |
| --- | --- |
| `10` and `10.5` | Stored as `10.00` and `10.50` |
| `"  2.50  "` | Parsed as `2.50` |
| `1.5` vs `1.50` | Equal, same hash code |
| Add/subtract cents | No floating-point drift |
| Zero | `isZero`, not `isPositive` |
| Negative, extra decimals, `abc`, null, empty, blank | `InvalidAmountException` |
| Subtract more than available | `InvalidAmountException` |
| `plus(null)` / `minus(null)` | `InvalidAmountException` |

---

## Account id

| Scenario | Expected |
| --- | --- |
| `" alice "` vs `"alice"` | Equal |
| Ordering | `"a"` < `"b"` |
| Null, empty, blank | `IllegalArgumentException` |
