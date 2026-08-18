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

- [x] T1 fetch -> map -> persist -> expose.
- [x] T2 remote failure -> cached data survives.
- [x] T3 remote + local unavailable -> relevant error.
- [x] Add only the cheap high-value checks documented in `specs/testing.md` as time permits; do not make them blockers.
- [x] Fix architecture issues exposed by tests.

**Verification (16 August 2026):** T1 now verifies the remote boundary is
called, the fake local boundary contains the committed entity snapshot, and
the repository exposes that snapshot as a domain article with stable identity,
publication time, and optional fields preserved. T2 verifies the domain
observation is unchanged after a classified network failure. T3 verifies the
intentional Phase 1 contract: an empty local observation remains
`ArticleObservation.Data(emptyList())`, while the failed initial synchronization
is `RefreshResult.Failure(SyncError.Network)`. These two outcomes are the
inputs Phase 4 presentation code must combine to derive the required blocking
`Error` state; a successful empty synchronization remains distinguishable by
`RefreshResult.Success` and is covered by the Phase 2 regression suite. No
production code or Phase 4 work was added in this review.

The clean verification gate passed with 38 Android shared tests, 39 iOS
shared tests, and 4 Android application unit tests; shared assemble/check,
both iOS framework links, Android debug assemble, and Android lint also passed.

**Exit:** all required unit/shared tests green.

## Phase 4 - Presentation

### Phase 4A - Article list presentation state and orchestration

- [x] Article list presentation state in `androidApp` (Compose screen remains Phase 4B).
- [x] Android ViewModel + StateFlow with typed durable content state and one-shot effects.
- [x] Initial loading.
- [x] Cached-content state.
- [x] Refresh orchestration for the future pull-to-refresh UI trigger.
- [x] Non-blocking refresh failure.
- [x] Empty state.
- [x] Blocking no-data error.

### Phase 4B - Compose article flows

- [x] Article list Compose screen and pull-to-refresh UI.
- [x] Article detail.
- [x] Navigation by stable article ID + both back paths.
- [x] Coil image loading.

**Phase 4A review verification (17 August 2026):** The ViewModel tests cover
V1–V5 plus deterministic regressions for late UI subscription, duplicate
refresh calls, local observation failure with and without cache, and
cancellation. The Android application test task passes 15 cases (5 state and
10 ViewModel); shared tests pass with 38 Android host-test cases and 39 iOS
simulator cases. The clean verification gate also passes shared assemble/check,
Android debug assemble, and Android lint. Phase 4B implementation followed in
the subsequent Compose work recorded below.

**Whole Phase 4 review (18 August 2026):** Phase 4B production flows are now
implemented. The review found and corrected blocking-error copy that was
otherwise hidden, raw detail error-string exposure, detail observation flow
creation during recomposition, optional-image semantics, and a double-decoded
percent-encoded navigation ID boundary. The clean gate passed 38 Android
shared tests, 39 iOS-simulator shared tests, 18 Android application unit
tests, shared assemble/check, Android debug assemble, and Android lint. The
Pixel_9 emulator launched the clean APK and reproduced the presentation-safe
no-cache offline error without a crash; the configured NewsAPI returned a
legitimate empty snapshot, so row/detail/cache runtime paths were not
re-exercisable in this environment. At this Phase 4 checkpoint, Phase 5 UI
tests had not started.

**Country configuration clarification (18 August 2026):** Inosoft clarified
that the PDF's `country=id` was illustrative and approved `country=us` because
it is currently the only non-empty country. The country now comes from the
existing `NewsApiConfig` boundary and is set to `us` by Android composition;
no selector or automatic fallback was added. Deterministic empty-state tests
remain independent of live NewsAPI results.

**Exit:** core flows are implemented, with prior online/offline manual evidence
recorded; the current clean-device run was limited by the configured endpoint
returning an empty snapshot, as documented above.

## Phase 5 - UI Tests

- [x] T4 app -> list -> detail.
- [x] T5 cached/offline state renders.
- [x] Inject deterministic repository/data boundaries for tests; never use the real NewsAPI.

**Verification (18 August 2026):** Implemented `FakeArticleRepository` and a
custom `AndroidJUnitRunner` providing `TestKabarKabarApp` to avoid production
Koin initialization. The focused review then seeded the fake persisted snapshot
before activity launch, configured the automatic refresh to return
`RefreshResult.Failure(SyncError.Network)`, and recorded that refresh was
actually called. T4 uses two distinguishable articles, taps the selected
article, and verifies the matching detail content through the real `NavHost`;
T5 verifies the cached article remains visible without a blocking retry state.
The tests use null image URLs and no real NewsAPI boundary. An Espresso
`NoSuchMethodException` on newer APIs was corrected by forcing
`androidx.test.espresso:espresso-core:3.7.0` and `androidx.test:runner:1.7.0`
via a Gradle `resolutionStrategy`.

T4 ran individually, T5 ran individually, and both ran together on the
`Pixel_9` emulator, for 2 instrumented UI tests with 0 failures. The clean
verification gate passed with 38 Android shared tests, 39 iOS shared tests,
18 Android application unit tests, and 2 Android instrumented UI tests;
shared assemble/check, Android debug assemble, and Android lint also passed.

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
