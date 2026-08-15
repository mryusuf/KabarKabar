# Implementation Plan

Deadline: **22 August 2026**.

The plan intentionally finishes core work before bonus work.

## Phase 0 - Scaffold and Guardrails

- [ ] Create KMP project + Android app.
- [ ] Finalize module/source-set names.
- [ ] Add non-negotiable dependencies.
- [ ] Add local API-key configuration and `.gitignore`.
- [ ] Confirm clean build.
- [ ] Confirm Android app launches.
- [ ] Update README with exact build commands.

**Exit:** clean build + empty app + no secret in source.

## Phase 1 - Domain + Offline Contract

- [ ] Define domain `Article`.
- [ ] Define domain error/result model.
- [ ] Define repository contract.
- [ ] Define refresh/cache semantics.
- [ ] Add use case(s) only where they add a clear boundary.

**Exit:** dependency direction is clear before UI implementation.

## Phase 2 - Data Layer

- [ ] NewsAPI DTOs and explicit mapping.
- [ ] Ktor client with timeout + logging configuration.
- [ ] Room KMP database/entity/DAO.
- [ ] Repository implementation.
- [ ] Persist successful usable remote data.
- [ ] Never delete valid cache on remote failure.

**Exit:** repository can synchronize and expose persisted data.

## Phase 3 - Write Critical Unit Tests Early

- [ ] T1 fetch -> map -> persist -> expose.
- [ ] T2 remote failure -> cached data survives.
- [ ] T3 remote + local unavailable -> relevant error.
- [ ] Fix architecture issues exposed by tests.

**Exit:** all required unit/shared tests green.

## Phase 4 - Presentation

- [ ] Article list UI.
- [ ] ViewModel + StateFlow.
- [ ] Initial loading.
- [ ] Cached-content state.
- [ ] Pull-to-refresh.
- [ ] Non-blocking refresh failure.
- [ ] Empty state.
- [ ] Blocking no-data error.
- [ ] Article detail.
- [ ] Navigation + both back paths.
- [ ] Coil image loading.

**Exit:** all core flows work manually online and offline.

## Phase 5 - UI Tests

- [ ] T4 app -> list -> detail.
- [ ] T5 cached/offline state renders.
- [ ] Make DI/data deterministic for tests.

**Exit:** required UI tests green and stable.

## Phase 6 - Review / Polish

- [ ] Run full test suite.
- [ ] Review recomposition/list performance.
- [ ] Review error messages and empty states.
- [ ] Review accessibility basics.
- [ ] Check naming and dead code.
- [ ] Verify API-key hygiene, including git history.
- [ ] Test setup instructions from a clean checkout.

**Exit:** core submission is shippable.

## Phase 7 - Bonus Only If Core Is Green

Priority order:

1. [ ] `commonTest` coverage / shared-target confidence
2. [ ] dark mode polish
3. [ ] iOS shared target or minimal iOS proof if low-risk
4. [ ] accessibility polish
5. [ ] pagination
6. [ ] full-screen image viewer

Do not add bonus modularization unless it solves a real problem.

## Suggested Calendar

### Aug 15
Scaffold, architecture, API-key handling.

### Aug 16
Ktor + Room + mappings + repository.

### Aug 17
Offline-first unit tests; fix repository semantics.

### Aug 18
Compose list/detail + ViewModel/StateFlow + navigation.

### Aug 19
Pull-to-refresh, errors/empty states, manual offline testing.

### Aug 20
UI tests + Android Studio Agent Mode evidence review.

### Aug 21
Clean-checkout validation, README, polish, secret/history audit.

### Aug 22
Buffer only: final regression, repository visibility, submission.

## Stop Conditions

Do not begin a bonus feature if any of these is red:

- build,
- required unit tests,
- required UI tests,
- offline cache fallback,
- README setup,
- secret hygiene,
- AI evidence.
