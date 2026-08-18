# Inosoft KMP News Reader

A production-minded offline-first News Reader built for the PT Inosoft Trans Sistem Mobile Developer take-home test.

> Status: Phases 0–4 presentation and runtime review are complete. Phase 5 Compose UI acceptance tests remain pending.

## Goal

Demonstrate:

- meaningful Kotlin Multiplatform code sharing,
- Clean Architecture,
- reliable offline-first behavior,
- maintainable Compose presentation,
- meaningful automated tests,
- responsible Android Studio Agent Mode usage.

## Planned Core Features

- [x] Article list: title, short description, image, publication date
- [x] Pull-to-refresh
- [x] Article detail screen
- [x] Loading state
- [x] Empty state
- [x] Cached content displayed when offline
- [x] Cached content remains visible after refresh failure
- [x] Non-blocking refresh error when cache exists
- [x] Clear error state when neither remote nor local data is usable
- [x] System back + app-bar back navigation
- [x] Required unit tests
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

Configure the root `local.properties` file, which is ignored by Git:

```properties
sdk.dir=/absolute/path/to/your/Android/sdk
NEWS_API_KEY=your_real_development_key
```

`androidApp/build.gradle.kts` reads `NEWS_API_KEY` locally and exposes it as
`BuildConfig.NEWS_API_KEY` for the application composition root. The Android
application composes the Phase 4 ViewModels, Room-backed repository, and
Navigation/Coil presentation flow. The shared Phase 1–3 domain/data behavior
is covered by shared tests.

The key must be treated as public client configuration: Android packages can be
inspected. Never log it, put it in shared domain state, or commit it to source
control. The repository contains only the setup instructions above.

### Submission country decision

The take-home PDF used `country=id` as a sample. During development it returned
no current articles, so Inosoft was contacted and explicitly approved
`country=us`, currently the only country returning non-empty data. The
submitted app therefore configures `us` so the complete online/offline flow can
be demonstrated. A country selector and automatic country fallback remain out
of scope; empty-state coverage uses deterministic test data.

## Build & Run

Run these commands from the repository root after configuring `local.properties`:

```bash
# Clean build of the Android app and shared KMP module.
./gradlew clean :androidApp:assembleDebug :sharedLogic:assemble

# Incremental Android debug build.
./gradlew :androidApp:assembleDebug

# Android unit task plus shared KMP tests.
./gradlew :androidApp:testDebugUnitTest :sharedLogic:allTests

# Static verification.
./gradlew :androidApp:lintDebug :sharedLogic:check

# Compose/instrumentation tests; requires an attached emulator or device.
./gradlew :androidApp:connectedDebugAndroidTest
```

The Android unit task currently runs 5 `ArticleListUiStateTest` cases, 10
`ArticleListViewModelTest` cases, 2 `ArticleDetailViewModelTest` cases, and 1
navigation boundary test (18 total). Compose UI test sources are intentionally
not added yet because they belong to Phase 5.

To manually verify the current Android application on a connected device:

```bash
adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
adb shell am start -n io.mryusuf.kabarkabar/.MainActivity
```

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

Durable content uses typed Loading, Data, Empty, and Error states. Refreshing is orthogonal to content availability, and one-shot Snackbar/effect messages are not persistent nullable StateFlow data. At the repository boundary, a failed initial refresh is returned as `RefreshResult.Failure` while an empty local snapshot remains `ArticleObservation.Data(emptyList())`; Phase 4 combines those outcomes to derive the blocking Error state, while `RefreshResult.Success` distinguishes a legitimate successful empty snapshot. Compose rendering and navigation are complete; formal UI acceptance remains Phase 5.

See `specs/architecture.md`.

## Testing

Required behavior and test scenarios are tracked in `specs/testing.md`.

Phase 3 acceptance coverage is green: T1 fetches, maps, persists, and exposes
an article; T2 preserves an existing cache after a network failure; and T3
returns a classified failure alongside an empty local observation when no
usable cache exists. The detailed evidence is in
`plan/implementation-plan.md` and `plan/ai-usage-log.md`.

The test suite must cover at least:

1. remote fetch -> mapping -> persistence -> cached data exposed,
2. remote failure -> cached data still exposed,
3. both remote and local unavailable -> error,
4. list -> tap article -> detail,
5. previously cached articles render offline.

Cheap high-value additions such as cache visibility during refresh, valid empty snapshots, failed transactional replacement, and stable ID/duplicate behavior are useful but are not additional implementation blockers.

## Android Studio Agent Mode Usage

The assessment requires at least **three meaningful tasks performed with Android Studio Agent Mode**.

This table reflects the actual tasks recorded in `plan/ai-usage-log.md`; Codex
review/remediation is kept separate from Agent Mode evidence.

| Task | Prompt/Goal | Agent Output | What I Changed or Rejected | Validation |
|---|---|---|---|---|
| 1 | Phase 0 scaffold, module ownership, dependencies, and API-key setup | See `plan/ai-usage-log.md` Entry 1 | Corrected generated Gradle accessor usage and deferred incompatible MockK test wiring | Phase 0 verification included clean assemble, lint/check, and Pixel 9 placeholder launch; the Android unit task was `NO-SOURCE` at that checkpoint |
| 2 | Phase 2 shared infrastructure and offline-first repository | See `plan/ai-usage-log.md` Entry 2 | Corrected the empty-vs-malformed mapper semantics, test dependency wiring, and Room constructor setup | Phase 2 checkpoint and later independent regression verification are recorded in Entry 2 |
| 3 | Phase 3 T1–T3 offline-first acceptance tests | See `plan/ai-usage-log.md` Entry 3 | Agent Mode corrected minor compile-time test issues; Codex review separately strengthened persistence and optional-field assertions | `:sharedLogic:allTests`: 38 Android shared tests and 39 iOS shared tests |
| 4 | Phase 4A ViewModel state orchestration and presentation tests | See `plan/ai-usage-log.md` Entry 4 | Agent Mode corrected the initial empty-vs-failed-sync state distinction; the later independent Codex review/remediation is not Agent Mode evidence | `:androidApp:testDebugUnitTest`: 15 cases after remediation |
| 5 | Phase 4B Compose screens, navigation, pull-to-refresh, and Coil image loading | See `plan/ai-usage-log.md` Entry 5 | Gemini-assisted image debugging recorded the missing Coil 3 network integration and singleton ImageLoader correction; independent review fixes remain separate | Debug assemble plus Pixel 9 exploratory runtime checks |

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
- [x] 3+ Android Studio Agent Mode tasks documented
- [x] At least 1 real AI mistake/correction documented
- [ ] Known issues stated explicitly
