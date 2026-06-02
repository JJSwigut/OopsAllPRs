# Contracts: Developer Demo Data Seeding

## Shared Use Case Contract

`DeveloperSeedUseCase` exposes named suspend functions for scenario loading:

- Load progress demo data.
- Load routine demo data.
- Load active recovery demo data.

Each function returns an explicit result:

- `Loaded`: Data was created.
- `Skipped`: Matching data already exists, so no duplicate was created.
- `Failed`: Scenario could not load, with a user-readable reason.

Contract rules:
- The use case must call existing exercise seed ingestion when required baseline exercises are missing.
- Completed workouts must be created through ordinary workout lifecycle and set logging use cases.
- PR and progress data must be derived through existing PR derivation behavior.
- Active recovery data must fail or skip if another active workout already exists.
- No function may require network access.

## Shared State Contract

`DeveloperSeedStateHolder` exposes state for Profile:

- Available scenario rows.
- Loading scenario id.
- Last status message.

Contract rules:
- State holder operations must be safe to call repeatedly.
- UI feedback must distinguish loaded, skipped, and failed outcomes.
- Refreshing app state after seed load is the caller's responsibility so History, Progress, Train, and resume banner can update.

## Android Debug Entry Contract

Android passes developer tooling availability into shared app construction.

Contract rules:
- Debug builds pass developer tooling enabled.
- Release builds use the default disabled path.
- Shared Profile UI renders the developer seed card only when state and callbacks are provided.
- No release-visible seed button, menu, shortcut, or automatic seed execution is allowed.
