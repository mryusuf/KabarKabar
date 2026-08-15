# Implementation Plan

Deadline: **22 August 2026**.

The plan intentionally finishes core work before bonus work.

## Phase 0 - Scaffold and Guardrails

- [x] Create KMP project + Android app.
- [x] Finalize target ownership: `sharedLogic` for shared domain/data and `androidApp` for all Android presentation/wiring.
- [x] Keep the existing `sharedUI` scaffold out of the target architecture; do not add new production code there.
- [x] Keep iOS UI out of core scope; retain an iOS shared target only if its platform construction remains low-risk and complete.
- [x] Add non-negotiable dependencies.
- [x] Add local API-key configuration and `.gitignore`.
- [x] Confirm clean build.
- [x] Confirm Android app launches.
- [x] Update README with exact build commands.

**Exit:** clean build + empty app + no secret in source.

**Verification (15 August 2026):** `clean :androidApp:assembleDebug :sharedLogic:assemble`,
`:androidApp:lintDebug :sharedLogic:check`, and `:sharedLogic:allTests` pass. The
debug APK was installed on the `Pixel_9` emulator and `MainActivity` displayed
the Phase 0 placeholder. `:androidApp:testDebugUnitTest` is currently
`NO-SOURCE` at the Phase 0 checkpoint; Android unit tests were intentionally
deferred to later phases and are now covered by the Phase 1 verification below.

## Phase 1 - Domain + Offline Contract

- [x] Define domain `Article` with canonical URL and stable deterministic URL-derived local ID.
- [x] Define domain error/result and typed observation models without exposing infrastructure exceptions.
- [x] Define the small repository contract: `observeArticles()`, `observeArticle(id)`, `refreshArticles()`.
- [x] Define typed durable content states: Loading, Data, Empty, Error; keep refreshing orthogonal.
- [x] Define refresh/cache/empty semantics, including valid empty snapshots and failed replacement behavior.
- [x] Confirm no pass-through use cases are needed at this boundary.

**Exit:** dependency direction is clear before UI implementation.

**Verification (15 August 2026):** Defined `ArticleId`, canonical URL validation,
`EpochMilliseconds`, domain refresh/observation outcomes, and the repository
contract in `sharedLogic/commonMain`. Defined `ArticleListUiState` and its
typed `Loading`/`Data`/`Empty`/`Error` content states in `androidApp`. Regression
tests cover ID determinism, ID mismatch rejection, empty-vs-failure semantics,
and cached data remaining visible while refreshing. No pass-through use cases
are needed at this stage. The clean verification command
`./gradlew clean :sharedLogic:allTests :sharedLogic:assemble :sharedLogic:check
:androidApp:testDebugUnitTest :androidApp:assembleDebug :androidApp:lintDebug`
passes, including Android unit tests, both iOS framework targets, and lint.

## Phase 2 - Data Layer

- [x] Phase 2A: Shared Infrastructure (Ktor, Room, Mappings, Platform Builders).
- [x] Phase 2B: Repository implementation and offline-first synchronization.
- [x] NewsAPI DTOs and explicit mapping.
- [x] Ktor shared API behavior with platform-specific engine construction, injected configuration, and redacted logging.
- [x] Room KMP database/entity/DAO with one DI-owned instance, platform builders, and checked-in schema output.
- [x] Repository implementation.
- [x] Validate/map the full remote response before transactionally replacing the cached headline snapshot.
- [x] Preserve the previous committed cache on remote, malformed-data, or database-replacement failure.
- [x] Define duplicate and ordering behavior.

**Exit:** repository can synchronize and expose persisted data.

**Review verification (15 August 2026):** End-to-end Phase 2 review fixed
timestamp parsing, NewsAPI envelope/error validation, persistence error
classification, local observation exception containment, entity identity
validation, duplicate resolution, and the iOS Room builder. The Room schema
output is present under `sharedLogic/schemas`. Shared tests pass
with 35 Android host-test cases and 36 iOS simulator-test cases; both iOS
device/simulator framework targets compile and link. The clean verification
command below also passes, including Android unit tests (4 cases), debug
assemble, and lint. Phase 3 remains intentionally unchecked.

```text
./gradlew clean \
  :sharedLogic:allTests \
  :sharedLogic:assemble \
  :sharedLogic:check \
  :androidApp:testDebugUnitTest \
  :androidApp:assembleDebug \
  :androidApp:lintDebug
```

## Phase 3 - Write Critical Unit Tests Early

- [ ] T1 fetch -> map -> persist -> expose.
- [ ] T2 remote failure -> cached data survives.
- [ ] T3 remote + local unavailable -> relevant error.
- [ ] Add only the cheap high-value checks documented in `specs/testing.md` as time permits; do not make them blockers.
- [ ] Fix architecture issues exposed by tests.

**Exit:** all required unit/shared tests green.

## Phase 4 - Presentation

- [ ] Article list UI in `androidApp`.
- [ ] Android ViewModel + StateFlow with typed durable content state and one-shot effects.
- [ ] Initial loading.
- [ ] Cached-content state.
- [ ] Pull-to-refresh.
- [ ] Non-blocking refresh failure.
- [ ] Empty state.
- [ ] Blocking no-data error.
- [ ] Article detail.
- [ ] Navigation by stable article ID + both back paths.
- [ ] Coil image loading.

**Exit:** all core flows work manually online and offline.

## Phase 5 - UI Tests

- [ ] T4 app -> list -> detail.
- [ ] T5 cached/offline state renders.
- [ ] Inject deterministic repository/data boundaries for tests; never use the real NewsAPI.

**Exit:** required UI tests green and stable.

## Phase 6 - Review / Polish

- [ ] Run full test suite.
- [ ] Review recomposition/list performance.
- [ ] Review error messages and empty states.
- [ ] Review accessibility basics.
- [ ] Check naming and dead code.
- [ ] Verify API-key hygiene, including git history.
- [ ] Verify target module/source-set ownership against `specs/architecture.md`.
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
