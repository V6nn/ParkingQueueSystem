# Parking Queue System — Project Guide

> **Purpose:** Shared technical reference for the three-person team and any AI coding assistant helping with this project.
>
> **Important:** This guide records the project structure and interfaces known at the time it was written. When exact method signatures or behavior matter, inspect the current Java source files rather than guessing. Update this guide when the team agrees on changes.

## 1. Project overview

**Project title:** Development of a Queueing Model to Predict Waiting Time and Optimize Entry Capacity in Parking Lots.

The application is a Java desktop application for a parking lot administrator. The administrator manually configures the parking lot and records occupancy and entrance/exit activity. The system stores historical records, estimates queueing and waiting times, recommends entry capacity and lane usage, and displays the results in an admin dashboard.

The project does **not** require physical sensors. During development, modules may use sample data, but the final integrated application should use the shared database and services.

## 2. Technology and repository

- **Language:** Java 21
- **Build:** Maven
- **IDE:** Visual Studio Code
- **Database:** SQLite
- **Database driver:** Xerial SQLite JDBC
- **Planned desktop UI:** Java Swing
- **Version control:** Git and GitHub

**GitHub repository:** https://github.com/V6nn/ParkingQueueSystem

**Maven project root:** `parkingqueuesystem/` (the folder containing `pom.xml`)

**Java package root:** `com.parking`

**Local path currently used by Person 1 (may differ on other computers):**

`C:\Users\AtlasOS\Desktop\ParkingQueueSystem\parkingqueuesystem`

Do not hardcode this Windows path in application code. Teammates should use their own local repository path.

### Build and test

Run these commands from the directory containing `pom.xml`:

```bash
mvn clean test
```

The project has successfully built with Maven in prior work. Service-layer save/retrieval checks also passed for all six services using temporary test data. Always rerun the build after code changes; do not assume the current checkout is identical.

## 3. Team roles and package ownership

### Person 1 — Database and data management

Responsible for:
- Database connection and initialization
- Model classes and database service layer
- Parking lot, entrance, and exit configuration
- Manual data entry and input validation
- Current and historical parking, entrance, and exit records
- Providing stored data to other modules

Packages:
- `com.parking.database`
- `com.parking.models`
- `com.parking.services`

### Person 2 — Queueing model and prediction

Responsible for:
- Queueing calculations
- Estimated waiting time
- Predicted queue length
- Service-time and arrival/entry-rate calculations
- Peak-hour or historical queue analysis, as supported by the available data
- A documented input/output contract for the optimization module

Packages:
- `com.parking.queueing`
- `com.parking.prediction`

### Person 3 — Optimization and dashboard

Responsible for:
- Recommended number of active entry lanes
- Recommended entry capacity
- Congestion detection and alerts
- Optimization rules and explanations
- Admin dashboard and presentation of current values, predictions, and recommendations

Packages:
- `com.parking.optimization`
- `com.parking.ui`

**UI coordination:** Person 3 owns the main dashboard. If a separate manual data-entry screen is needed, coordinate with Person 1 and use a separate class/screen rather than overwriting the dashboard. Agree on UI class ownership before editing shared UI files.

## 4. Expected source structure

This is the intended high-level organization, not a guarantee that every package already contains committed files.

```text
src/
├── main/
│   └── java/
│       └── com/
│           └── parking/
│               ├── App.java
│               ├── database/
│               │   ├── DatabaseConnection.java
│               │   └── DatabaseInitializer.java
│               ├── models/
│               │   ├── ParkingLot.java
│               │   ├── Entrance.java
│               │   ├── Exit.java
│               │   ├── ParkingRecord.java
│               │   ├── EntranceRecord.java
│               │   └── ExitRecord.java
│               ├── services/
│               │   ├── ParkingLotService.java
│               │   ├── EntranceService.java
│               │   ├── ExitService.java
│               │   ├── ParkingRecordService.java
│               │   ├── EntranceRecordService.java
│               │   └── ExitRecordService.java
│               ├── queueing/       # Person 2
│               ├── prediction/     # Person 2
│               ├── optimization/   # Person 3
│               └── ui/             # Person 3; coordinate data-entry screens
└── test/
    └── java/
        └── ...
```

Git does not track empty directories. A package will appear in the repository only after it contains a tracked file.

## 5. Database connection and dependencies

The current database connection uses the JDBC URL:

```java
jdbc:sqlite:parking.db
```

Use the existing `com.parking.database.DatabaseConnection` class to obtain connections. Do not create a second database-connection utility or scatter connection URLs throughout the code.

The connection enables SQLite foreign-key enforcement using:

```sql
PRAGMA foreign_keys = ON
```

The Maven project uses the Xerial SQLite JDBC driver. Check the current `pom.xml` before changing dependencies. The dependency currently recorded in the project is:

```xml
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>3.50.3.0</version>
</dependency>
```

Do not modify `pom.xml` or database schema without discussing the change with the team first.

## 6. Database tables

The initializer creates the following tables. Confirm the exact schema in `DatabaseInitializer.java` before writing SQL or relying on column types, constraints, or defaults.

### `parking_lot`

Known columns:
- `id`
- `name`
- `total_spaces`
- `operating_start`
- `operating_end`

### `entrances`

Known columns:
- `id`
- `name`
- `lane_count`
- `active_lanes`
- `status`

### `exits`

Known columns:
- `id`
- `name`
- `lane_count`
- `active_lanes`
- `status`

### `parking_records`

Known columns:
- `id`
- `recorded_at`
- `occupied_spaces`
- `available_spaces`

### `entrance_records`

Known columns:
- `id`
- `entrance_id`
- `recorded_at`
- `queue_length`
- `vehicles_entered`
- `average_service_time`

### `exit_records`

Known columns:
- `id`
- `exit_id`
- `recorded_at`
- `vehicles_exited`

### `congestion_rules`

Known columns:
- `id`
- `rule_name`
- `threshold`
- `unit`
- `description`
- `enabled`

### `recommendations`

Known columns:
- `id`
- `entrance_id`
- `created_at`
- `current_queue`
- `recommended_lanes`
- `recommended_capacity`
- `reason`

SQLite may also create `sqlite_sequence` automatically for auto-incrementing IDs.

Known behavior:
- Entrance and exit status values are expected to be `OPEN` or `CLOSED`.
- Each connection enables foreign-key support.
- `ParkingRecordService` calculates available spaces as total spaces minus occupied spaces when saving.
- Entrance/exit service validation checks lane counts, active-lane limits, and status.
- Do not assume that the database alone enforces every business rule; inspect the initializer and service classes.

**Design note to resolve later:** the `parking_lot` table may allow multiple rows even though the current application is described as managing one parking lot. Decide with the team whether to enforce a single-lot rule in the application or revise the schema in a coordinated change.

## 7. Existing model classes

Person 1 has created these model classes under `com.parking.models`:

- `ParkingLot`
- `Entrance`
- `Exit`
- `ParkingRecord`
- `EntranceRecord`
- `ExitRecord`

They contain fields, constructors, getters, and setters for their corresponding records. **Always inspect the actual class before assuming constructor order, field types, or available methods.** Do not create duplicate model classes in other packages.

## 8. Existing service layer

Person 1 has created these services under `com.parking.services`:

| Service | Known operations |
|---|---|
| `ParkingLotService` | Save, update, find by ID, find all |
| `EntranceService` | Save, update, find by ID, find all, delete |
| `ExitService` | Save, update, find by ID, find all, delete |
| `ParkingRecordService` | Save using total capacity, find all, find latest |
| `EntranceRecordService` | Save, find all, find by entrance ID, find latest by entrance ID |
| `ExitRecordService` | Save, find all, find by exit ID, find latest by exit ID |

Important interface notes:
- `ParkingLotService.save(ParkingLot)` returns an `int`.
- `ParkingLotService.findById(int)` returns an `Optional`.
- Entrance/exit `save` operations have been implemented as `void`; check the source for the exact signatures.
- Some lookup methods return `Optional`, some return lists, and `ParkingRecordService.findLatest()` returns `null` when no record exists.
- The record-save methods validate some inputs and set the saved record ID.
- Read the current source before calling any method. This guide intentionally does not reproduce every method signature.

### Service-layer check already performed

A temporary test runner successfully checked saving and retrieving:
- A parking lot
- An entrance
- An exit
- A parking record
- An entrance record
- An exit record

The runner reported all six checks passed and temporary test data was cleaned up. Do not assume that the temporary runner is a permanent automated JUnit test. Check that it was removed from `src/main/java` before release; permanent tests should be placed under `src/test/java`.

## 9. End-to-end system workflow

1. Administrator configures parking capacity, entrances, exits, lane counts, active lanes, and open/closed status.
2. Administrator enters parking occupancy and entrance/exit activity manually.
3. The service layer validates and stores records in SQLite.
4. Person 2's queueing/prediction module reads the relevant records or receives values through an agreed interface.
5. Person 2 calculates queue estimates and waiting-time estimates and documents assumptions or warnings.
6. Person 3's optimization module receives the queueing results and current parking/entrance configuration.
7. The optimization module returns recommended lane usage, capacity, and congestion reasons.
8. The dashboard displays the current records, predictions, and recommendations.
9. Where applicable, the system stores recommendations and historical data for later analysis.

Avoid claiming real-time sensor readings. The project uses manually entered data; “current” values mean the latest saved values available to the application.

## 10. Integration contracts to agree on

The team must agree on these interfaces before writing code that connects the modules. The exact Java classes/methods below are **not yet finalized**.

### Person 1 → Person 2: data inputs

Potential inputs:
- Total parking spaces
- Latest occupied and available spaces
- Entrance ID and open/closed status
- Total and active lanes per entrance
- Latest queue length for an entrance
- Vehicles entered over a defined time interval
- Average service time and its unit
- Historical entrance records with timestamps
- Time interval used to calculate entry/arrival rates

Person 1 and Person 2 must decide how data is passed: direct service calls, a small data-transfer object, or another agreed interface. Do not make Person 2 execute ad hoc SQL or duplicate database access.

### Person 2 → Person 3: prediction outputs

Potential outputs:
- Entrance ID
- Current/observed queue length
- Predicted queue length
- Estimated waiting time
- Estimated arrival/entry rate and its unit
- Estimated service rate and its unit
- Time horizon or timestamp of the prediction
- Data-sufficiency or calculation warnings

Person 2 must document the calculation assumptions, units, and edge cases, such as zero service rate, missing records, or closed entrances.

### Person 3: optimization inputs and outputs

Potential inputs:
- Current entrance status
- Total and active lanes
- Parking spaces available
- Current and predicted queue length
- Estimated waiting time and rates
- Agreed congestion thresholds

Potential outputs:
- Recommended number of active lanes
- Recommended entry capacity over a clearly defined time interval
- Congestion status or alert
- Human-readable reason for each recommendation
- Timestamp and entrance ID

The optimization logic must respect available parking capacity, entrance status, lane limits, and the assumptions of the queueing model. The team must define what “entry capacity” means (for example, vehicles per minute or per hour) and use that unit consistently.

### Shared rules for data and calculations

- Agree on time units and timestamps; do not mix seconds, minutes, and hours without conversion.
- Agree on whether rates are measured per minute or per hour.
- Handle missing data explicitly instead of treating it as zero.
- Treat a closed entrance as unavailable for new entries.
- Do not recommend more active lanes than the entrance physically has.
- Do not recommend entry volume that ignores available parking spaces or other agreed constraints.
- Avoid duplicate or contradictory calculations in different modules.
- Display when values are estimates and identify stale or insufficient data.

## 11. Git collaboration workflow

Branches:
- Person 1: `person1-data`
- Person 2: `person2-queueing`
- Person 3: `person3-optimization`
- `main`: shared integration branch

Each teammate should:
1. Open the repository's Maven project root.
2. Run `git status` and `git branch --show-current`.
3. Confirm they are on their own branch before editing.
4. Fetch/pull the appropriate branch before starting work.
5. Work on their assigned files and coordinate before editing shared files.
6. Run `mvn clean test` from the directory containing `pom.xml`.
7. Review `git diff` and `git status`.
8. Commit with a descriptive message.
9. Push to their own branch.
10. Open a pull request or coordinate the merge into `main` with the designated integrator.

Useful commands (after confirming the intended branch):

```bash
git status
git branch --show-current
git fetch origin
mvn clean test
git diff
```

To update a local branch, first confirm the correct branch and repository state, then use the appropriate pull command for that branch. Do not blindly reset, force-push, or discard local changes. If Git reports conflicts, stop and resolve them deliberately.

Coordinate changes to shared files, especially:
- `pom.xml`
- Database initializer and connection
- Model classes
- Service classes
- `App.java`
- UI entry points

## 12. AI-assisted coding rules

Any AI assistant helping a teammate must:

1. Identify which team role the user is working on.
2. Inspect supplied source files before relying on method names, constructors, or field types.
3. Ask for missing source files when exact compatibility cannot otherwise be verified.
4. Reuse the existing classes and interfaces instead of creating duplicates.
5. Use Java 21-compatible code and the existing Maven layout.
6. Make one small, clearly scoped change at a time.
7. State the exact path for each new or modified file.
8. Keep SQL in the data/service layer, not in Swing event handlers.
9. Use prepared statements for SQL values.
10. Separate persistence, queueing calculations, optimization logic, and UI responsibilities.
11. Avoid changing shared dependencies or database schema without team agreement.
12. Include clear input validation and error handling.
13. Give the exact build/test command and help interpret actual results.
14. Never claim that code compiled or tests passed unless the user actually ran them and supplied the result.
15. Preserve unrelated code and warn before suggesting destructive Git commands.

AI-generated code is a proposal until it has been reviewed, compiled, and tested in the actual shared project.

## 13. Testing and definition of done

A feature is not complete just because its source code exists.

For each feature:
- Compile and run tests with `mvn clean test`.
- Test valid inputs and invalid/boundary cases.
- Confirm missing records and database errors are handled sensibly.
- Confirm the correct branch contains the change and it has been pushed.
- Document any new public method or data contract.
- Ensure the rest of the team knows what changed.

### End-to-end acceptance scenario

1. Configure a parking lot with a known total capacity.
2. Add at least one open entrance and one exit with valid lane counts.
3. Save a parking occupancy record and valid entrance/exit activity records.
4. Confirm the records can be retrieved from the services.
5. Run the queueing model and verify its output against a small hand-calculated example.
6. Run optimization and verify it does not exceed physical lane limits or available parking capacity.
7. Open the dashboard and confirm the values match the module outputs.
8. Test missing history, zero service rate, a closed entrance, a full parking lot, and invalid manual input.
9. Restart the application and verify saved data persists in SQLite.
10. Run `mvn clean test` after integration and resolve all failures before demonstrating the application.

## 14. Decisions and work still to finalize

Keep this section updated as the team makes decisions.

- [ ] Confirm all six service classes and the removal of the temporary service test runner are committed and pushed.
- [ ] Confirm the current exact model constructors and service method signatures from source.
- [ ] Agree whether the app enforces one parking lot or supports multiple lots.
- [ ] Agree on timestamp formats and rate/time units.
- [ ] Person 2 documents the queueing model, assumptions, inputs, and outputs.
- [ ] Person 3 documents the optimization inputs, constraints, and outputs.
- [ ] Agree on the boundary between Person 1's manual data-entry work and Person 3's UI ownership.
- [ ] Agree on a designated integrator and merge/review process.
- [ ] Add permanent automated tests for service, queueing, optimization, and end-to-end behavior.
- [ ] Complete and verify the end-to-end acceptance scenario.

---

**Golden rule:** The actual committed Java source is the source of truth for code interfaces. This guide explains the shared design, but no teammate or AI assistant should guess an exact signature that has not been verified in the repository.
