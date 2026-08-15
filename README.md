# Inosoft KMP News Reader

A production-minded offline-first News Reader built for the PT Inosoft Trans Sistem Mobile Developer take-home test.

> Status: starter documentation. Replace placeholders and keep this README accurate as implementation progresses.

## Goal

Demonstrate:

- meaningful Kotlin Multiplatform code sharing,
- Clean Architecture,
- reliable offline-first behavior,
- maintainable Compose presentation,
- meaningful automated tests,
- responsible Android Studio Agent Mode usage.

## Planned Core Features

- [ ] Article list: title, short description, image, publication date
- [ ] Pull-to-refresh
- [ ] Article detail screen
- [ ] Loading state
- [ ] Empty state
- [ ] Cached content displayed when offline
- [ ] Cached content remains visible after refresh failure
- [ ] Non-blocking refresh error when cache exists
- [ ] Clear error state when neither remote nor local data is usable
- [ ] System back + app-bar back navigation
- [ ] Required unit tests
- [ ] Required Compose UI tests

## Tech Stack

- Kotlin + Kotlin Multiplatform
- Jetpack Compose
- ViewModel + StateFlow
- Navigation Compose
- Ktor Client
- Room for KMP
- Koin or another KMP-compatible DI approach, composed from `androidApp`
- Coil
- Gradle Kotlin DSL
- JUnit + MockK/equivalent
- Compose UI Testing

## Architecture

```text
androidApp / Presentation + Composition Root
        |
        v
sharedLogic / Domain  <----- sharedLogic / Data
  models                         Ktor API
  repository contracts            Room DB
  meaningful use cases            mappers
                                  repository implementation
```

The existing `sharedUI` module is not part of the target architecture. Android presentation remains in `androidApp`; iOS UI is outside core scope.

Offline-first data flow:

```text
Compose UI
   |
   | observes StateFlow
   v
ViewModel / Use Case
   |
   v
Repository
   |
   +---- observes persisted Room data (exclusive read source) ----> UI state
   |
   +---- refreshes NewsAPI
              |
              v
            Room
              |
              +------------------------------> updated UI
```

Network responses never feed UI directly. Refresh fetches first, validates/maps the full response, and transactionally replaces the persisted headline snapshot only after successful validation.

See:

- `specs/architecture.md`
- `specs/requirements.md`
- `specs/testing.md`

## Source-Set Intent

```text
sharedLogic/
  commonMain/   domain + shared data/business logic
  androidMain/  Android HTTP engine and Room database construction
  iosMain/      iOS construction only if the shared target remains in scope

androidApp/
  Compose UI + ViewModels + Navigation
```

Platform-specific code should stay minimal.

## NewsAPI Setup

Do **not** commit a real API key.

Recommended local configuration:

1. Put your development key in the `local.properties`:

```properties
NEWS_API_KEY=your_real_development_key
```

2. Ensure `local.properties` is ignored by Git.
3. Wire the value into the build/runtime configuration without placing the secret in source code.

Document the final mechanism here once implemented.

## Build & Run

Add exact clean-checkout commands after the Gradle module names are finalized.

Expected final documentation should include commands for:

- building the Android app,
- running unit tests,
- running Compose UI tests,
- any shared/common tests.

## Offline-First Decisions

- Room is the exclusive readable source of article data for higher layers.
- Successful remote synchronization writes to Room.
- UI observes local persisted state rather than rendering the raw network response.
- The repository contract is `observeArticles()`, `observeArticle(id)`, and `refreshArticles()`.
- Each article has a stable deterministic local ID derived from its canonical URL; Navigation passes the ID, not the whole article.
- A valid successful empty response may replace the previous snapshot and produces the Empty state.
- Existing cache survives remote refresh failure.
- Malformed/unusable remote data and failed database replacement preserve the previous committed cache.
- A refresh failure with cache is shown as a non-blocking message.
- No cache + no usable remote/local data becomes an explicit error state.

Durable content uses typed Loading, Data, Empty, and Error states. Refreshing is orthogonal to content availability, and one-shot Snackbar/effect messages are not persistent nullable StateFlow data.

See `specs/architecture.md`.

## Testing

Required behavior and test scenarios are tracked in `specs/testing.md`.

The test suite must cover at least:

1. remote fetch -> mapping -> persistence -> cached data exposed,
2. remote failure -> cached data still exposed,
3. both remote and local unavailable -> error,
4. list -> tap article -> detail,
5. previously cached articles render offline.

Cheap high-value additions such as cache visibility during refresh, valid empty snapshots, failed transactional replacement, and stable ID/duplicate behavior are useful but are not additional implementation blockers.

## Android Studio Agent Mode Usage

The assessment requires at least **three meaningful tasks performed with Android Studio Agent Mode**.

Do not fabricate this section. Populate it from `plan/ai-usage-log.md` after the tasks actually happen.

| Task | Prompt/Goal | Agent Output | What I Changed or Rejected | Validation |
|---|---|---|---|---|
| 1 | TBD | TBD | TBD | TBD |
| 2 | TBD | TBD | TBD | TBD |
| 3 | TBD | TBD | TBD | TBD |

At least one final entry must describe a real AI-generated mistake/suboptimal approach and how it was corrected.

## Key Technical Decisions

Keep this short and concrete. Suggested decisions to document as implementation settles:

- why Room is the UI source of truth,
- refresh/cache semantics,
- repository boundary and error model,
- stable URL-derived article identity and ID-based navigation,
- typed content state versus one-shot UI effects,
- DI choice,
- where platform-specific code is unavoidable,
- whether an iOS shared target is included,
- any deliberate simplifications.

## Bonus Features

Do not start bonuses until core behavior and tests are green.

Potential bonuses:

- dark mode,
- commonTest coverage,
- shared iOS target / iOS app if practical,
- pagination,
- full-screen image viewer,
- accessibility polish.

## Known Issues / Future Improvements

Populate honestly before submission.

## Submission Checklist

- [ ] Public GitHub/GitLab repository
- [ ] Clean checkout is runnable after API-key setup
- [ ] No real API key in repository or git history
- [ ] Required behavior works
- [ ] Required tests pass
- [ ] README setup instructions verified on a clean state
- [ ] Architecture and trade-offs documented
- [ ] 3+ Android Studio Agent Mode tasks documented
- [ ] At least 1 real AI mistake/correction documented
- [ ] Known issues stated explicitly
