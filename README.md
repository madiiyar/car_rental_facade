# Car Rental: Facade over a three-stage chain

A Facade layer over three independent subsystems (A: fleet, B: coverage, C: handover) with
durable SQLite state and compensation (rollback) when a later step fails.

## Run
Requires JDK 17 or newer (tested on JDK 21) and Maven 3.9+.
- Build, run all tests and the coverage check: `mvn verify`
- Run the demo application: `mvn -q compile exec:java` (or run `com.carrental.Main` from an IDE)
- Reset persisted state: choose option 10 in the menu, or delete the `data/` folder.
- Coverage report: `target/site/jacoco/index.html`
- Standalone subsystem demos: `FleetDemo`, `CoverageDemo`, `HandoverDemo` in `com.carrental.demo` (run their `main`).
- Persisted state (SQLite): `data/fleet.db`, `data/coverage.db`, `data/handover.db`, `data/chains.db`
- 
## Architecture
| Subsystem | Package | Logic | Entity (status) | Gateway |
|---|---|---|---|---|
| A: fleet availability | `fleet` | `PricingEngine`, `FleetAvailabilityTracker` | `Reservation` (HELD, CONFIRMED, RELEASED) | `ReservationRepository` |
| B: coverage | `coverage` | `InsuranceCalculator`, `DepositHandler` | `Deposit` (AUTHORIZED, CAPTURED, RELEASED) | `DepositRepository` |
| C: handover | `handover` | `DamageInspector`, `GpsTracker`, `HandoverService` | `Handover` (PENDING, ACTIVE, CANCELLED) | `HandoverRepository` |

Each subsystem has its own SQLite file (`fleet.db`, `coverage.db`, `handover.db`), so there is no
shared transaction and compensation is genuinely required. Subsystems never import each other;
they only meet in the facades. Each entity enforces its own legal status transitions.

- `RentalFacade.rentCar(...)`: runs A -> B -> C; on failure compensates and rethrows the original error.
- `RentalAdminFacade`: list/inspect chains, manual compensation, recovery of stuck chains after a
  restart, and controls used by the demo to break steps on demand.
- `ChainCompensator`: the single place where rollback lives (used by both facades).

### Persisted chain progress
The chain record (`rental_chains` table) holds the status: `PENDING_B` (A committed), `PENDING_C`
(A and B committed), `COMPLETED`, `COMPENSATED`. Compensation re-reads the chain from disk:
- `PENDING_B`: release the reservation (A) alone.
- `PENDING_C`: release the deposit (B), then the reservation (A).

Each undo step is skipped if the entity is already `RELEASED`, so a rollback interrupted by a
crash can be safely repeated (`recoverStuckChains`).

### Failure cases
| Case | Where | Result |
|---|---|---|
| Car unknown/booked, invalid dates | step A | exception, nothing committed, no chain created |
| Card limit too low | step B | A compensated |
| Damage found / GPS offline | step C | B then A compensated (GPS case leaves C record `CANCELLED`) |

## Principle of Least Knowledge: before / after
Before: the client must know three subsystems, their order, and how to roll back.
```java
Reservation r = fleet.reserve(carId, customerId, start, end);                 // friend 1
try {
    long days = ChronoUnit.DAYS.between(r.getStartDate(), r.getEndDate());    // reaches into Reservation
    Deposit d = coverage.authorize(r.getId(), customerId, plan, days);        // friend 2
    handover.startHandover(r.getId(), carId);                                 // friend 3
    fleet.confirm(r.getId());
} catch (RuntimeException e) {
    // the client must work out what to undo and in which order
}
```
After: one immediate friend.
```java
RentalChain chain = rentalFacade.rentCar(customerId, carId, start, end, plan);
```

## Why Facade and not Adapter
An Adapter exists to make an existing interface match the one a client already expects, so its
purpose is compatibility. Our subsystems are not incompatible with anything: the client has no
expected interface to be matched. The problem is complexity and coordination: three independent
subsystems must be called in the right order, their progress must be persisted, and a failure in
the middle must undo exactly the right part. A Facade gives clients a new, simpler, higher-level
operation (`rentCar`) and owns that orchestration, which an Adapter (a translator for one
interface) cannot do. The subsystems also stay directly usable, as with a Facade, whereas an
Adapter replaces an interface the client cannot use.

## Tests and coverage
- JUnit 5, F.I.R.S.T.: every test uses its own `@TempDir` with real SQLite files; no shared state.
- Domain-specific testing language: `RentalScenario` (e.g. `world.aCarIsAvailable("car-1")`,
  `world.rentingFailsWith(...)`, `world.afterRestart()`).
- Both compensation depths are tested (`RentalFacadeTest`), plus a persistence-reload test that
  simulates a restart in the middle of a chain (`RentalAdminFacadeTest`).
- JaCoCo requires >= 80% line coverage per package for `fleet`, `coverage`, `handover`, `facade`
  (`mvn verify`). Excluded from the check and report: standalone `*Demo` mains, the console UI
  (`cli`, `Main`), and the thin `infra` plumbing, because the requirement concerns the three
  subsystems and the facades.

## UML
See `docs/uml.png`.