# Implementation Plan

Deadline: **22 August 2026**.

The plan intentionally finishes core work before bonus work.

## Phase 0 - Scaffold and Guardrails

- [ ] Create KMP project + Android app.
- [ ] Finalize target ownership: `sharedLogic` for shared domain/data and `androidApp` for all Android presentation/wiring.
- [ ] Keep the existing `sharedUI` scaffold out of the target architecture; do not add new production code there.
- [ ] Keep iOS UI out of core scope; retain an iOS shared target only if its platform construction remains low-risk and complete.
- [ ] Add non-negotiable dependencies.
- [ ] Add local API-key configuration and `.gitignore`.
- [ ] Confirm clean build.
- [ ] Confirm Android app launches.
- [ ] Update README with exact build commands.

**Exit:** clean build + empty app + no secret in source.

## Phase 1 - Domain + Offline Contract

- [ ] Define domain `Article` with canonical URL and stable deterministic URL-derived local ID.
- [ ] Define domain error/result model without exposing infrastructure exceptions.
- [ ] Define the small repository contract: `observeArticles()`, `observeArticle(id)`, `refreshArticles()`.
- [ ] Define typed durable content states: Loading, Data, Empty, Error; keep refreshing orthogonal.
- [ ] Define refresh/cache/empty semantics, including valid empty snapshots and failed replacement behavior.
- [ ] Add use case(s) only where they add a meaningful domain/presentation boundary.

**Exit:** dependency direction is clear before UI implementation.

## Phase 2 - Data Layer

- [ ] NewsAPI DTOs and explicit mapping.
- [ ] Ktor shared API behavior with platform-specific engine construction, injected configuration, and redacted logging.
- [ ] Room KMP database/entity/DAO with one DI-owned instance, platform builders, and checked-in schema output.
- [ ] Repository implementation.
- [ ] Validate/map the full remote response before transactionally replacing the cached headline snapshot.
- [ ] Preserve the previous committed cache on remote, malformed-data, or database-replacement failure.
- [ ] Define stable ID, canonical URL, duplicate, and ordering behavior.

**Exit:** repository can synchronize and expose persisted data.

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
