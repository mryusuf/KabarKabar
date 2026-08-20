# Inosoft KMP News Reader

A production-minded offline-first News Reader built for the PT Inosoft Trans Sistem Mobile Developer take-home test.

> Status: Phases 0–6 remain protected by `release-v1.0.0`. Phases 7A–7E pass the local final audit after remediation; commit/push and final clean-clone verification are still required before release approval.

## Goal

Demonstrate:

- meaningful Kotlin Multiplatform code sharing,
- Clean Architecture,
- reliable offline-first behavior,
- maintainable Compose presentation,
- meaningful automated tests,
- responsible Android Studio Agent Mode usage.

## Core Features

- [x] Article list: title, short description, image, publication date
- [x] Pull-to-refresh
- [x] Article detail screen
- [x] Loading state
- [x] Empty state
- [x] Cached content displayed when offline
- [x] Cached content remains visible after refresh failure
- [x] Non-blocking refresh error when cache exists
- [x] Clear error state when neither remote nor local data is usable
- [x] Full-screen image viewer
- [x] System back + app-bar back navigation
- [x] Required unit tests
- [x] Required Compose UI tests

## Phase 7A Review Status

- [x] System-driven light/dark theme and system-bar contrast
- [x] Prominent first article without `Featured` semantics
- [x] Compact later rows with stable article-ID keys
- [x] Optional image/description-safe List and Detail layouts
- [x] Prominent hero fallback while an image is loading, unavailable, or missing
- [x] Loading, Empty, blocking Error, and cached refresh-failure presentation
- [x] Accessibility baseline and larger-font smoke check
- [x] Clean Android/shared test and Pixel_9 runtime gate

The Codex 7A review made presentation-only changes. `sharedLogic` data/domain
contracts remain unchanged, and the immutable `release-v1.0.0` tag is retained as
the fallback. No new Android Studio Agent Mode task was performed for this review;
the existing AI usage log remains the source of prior Agent Mode evidence.

## Phase 7C Review Status

- [x] Exact shared `US("us")` and `ID("id")` country model
- [x] compact accessible `🇺🇸 US` / `🇮🇩 ID` dropdown, defaulting to US on a new launch
- [x] Explicit country-scoped remote, Room, list, refresh, and detail paths
- [x] Composite Room identity and non-destructive v1 -> v2 migration
- [x] C1–C7 state, concurrency, and migration regression coverage
- [x] Pixel_9 country-switch and migration runtime tests

The 7C review found and corrected an unconstrained string country boundary and a
ViewModel race where a late refresh from the previous country could block or
classify the newly selected country. Country selection is not persisted across
launches; a new launch defaults to US. ID may legitimately show Empty when
NewsAPI has no current headlines, without affecting the US cache.

## Phase 7D Review Status

- [x] NewsAPI `page=1`/`pageSize=20` refresh and next-page requests
- [x] response metadata validation and country-scoped has-more state
- [x] transactional persisted append with stable-ID first-wins dedupe
- [x] refresh/page separation and stale-response invalidation
- [x] duplicate-trigger, refresh/page, country/page, and late-response tests
- [x] non-blocking progress/failure footer and end-of-list trigger
- [x] D1–D6 regression coverage plus Pixel_9 pagination UI tests

The 7D review found that a mutex alone did not prevent stale ViewModel state or
a page response from appending after a newer refresh began. Paging now uses
country-scoped operation generations, ViewModel job tokens, and a Room
transaction with conflict-ignore inserts. A failed page leaves the visible list
intact and returns to a simple `Load more` action; it never becomes a blocking
list error.

## Phase 7E Review Status

Phase 7E is a local candidate, not a final release. SwiftUI owns presentation
only. `IOSDependencyContainer` supplies the shared `ArticleRepository`, and
`FlowWrapper` exposes cancellable shared observations on the main dispatcher.
Swift does not implement NewsAPI requests, Room/SQL persistence, country cache
isolation, stable article IDs, or paging/dedupe.

If the ignored iOS `Config.local.xcconfig` is absent or its key is blank, the
native UI intentionally observes the existing Room cache without attempting a
remote sync. Configure the local key to enable fresh headlines and paging.

- [x] Apple simulator/device framework links and Xcode app build
- [x] native list, prominent first row, detail, back navigation, and viewer
- [x] shared US/ID selector and country-scoped repository calls
- [x] shared paging calls with a non-blocking footer failure path
- [x] loading, empty, blocking error, and cached refresh-failure presentation
- [x] cache-only startup when local iOS API configuration is absent
- [x] left thumbnails for later rows when image URLs are available
- [x] cancellable Flow bridge with weak Swift callbacks and operation generations
- [x] local ignored iOS API-key configuration and reproducible shared schemes
- [x] two deterministic iOS launch/country UI tests; broader iOS flow tests remain limited
- [x] Simulator Computer Use QA for list/detail/back/viewer/country/cache failure/theme/Dynamic Type
- [x] local final Phase 7 audit and full Android/iOS regression gates
- [ ] commit/push, final clean-clone verification, and release decision

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

The existing `sharedUI` module is not part of the target architecture. Android
presentation remains in `androidApp`; the native iOS UI is a thin Phase 7E
bonus layer over the shared repository.

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

```bash
cp local.properties.example local.properties
```

Replace the example SDK path and API key in the copied file:

```properties
sdk.dir=/absolute/path/to/your/Android/sdk
NEWS_API_KEY=your_real_development_key
```

`androidApp/build.gradle.kts` reads `NEWS_API_KEY` locally and exposes it as
`BuildConfig.NEWS_API_KEY` for the application composition root. The Android
application composes the Android-owned ViewModels, Room-backed repository, and
Navigation/Coil presentation flow. The shared Phase 1–3 domain/data behavior
is covered by shared tests.

The key must be treated as public client configuration: Android packages can be
inspected. Never log it, put it in shared domain state, or commit it to source
control. The repository contains only the setup instructions above.

## iOS Setup & Run

### Prerequisites

- macOS with Xcode 15.0+ installed.
- Android Studio with the KMP project open.
- A configured local iOS xcconfig; `local.properties` is still used by Gradle and Android.

### Build & Run

1. Copy the ignored iOS configuration and fill it locally:

    ```bash
    cp iosApp/Configuration/Config.local.xcconfig.example \
      iosApp/Configuration/Config.local.xcconfig
    # Edit Config.local.xcconfig and set NEWS_API_KEY to a local key.
    ```

    Do not edit `Koin.swift` or commit the local configuration. Xcode reads the
    key from the generated Info.plist setting; `local.properties` is not read
    automatically by Xcode.
2. Build the shared Apple frameworks:

    ```bash
    ./gradlew :sharedLogic:linkDebugFrameworkIosSimulatorArm64 \
      :sharedLogic:linkDebugFrameworkIosArm64
    ```

3. Build the app or open `iosApp/iosApp.xcodeproj` in Xcode:

    ```bash
    xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
      -destination 'platform=iOS Simulator,name=iPhone 17 Pro Max' build
    ```

4. Run the deterministic launch test scheme:

    ```bash
    xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosAppTests \
      -destination 'platform=iOS Simulator,name=iPhone 17 Pro Max' test
    ```

### iOS Architecture

The iOS application uses native **SwiftUI** for presentation.
- **SharedLogic**: Provides the `ArticleRepository`, `Article` domain models,
  country identity, Room-backed offline-first repository, Ktor synchronization,
  paging/dedupe, and `FlowWrapper` for observation.
- **Swift adapters**: `ArticleListViewModel` and `ArticleDetailViewModel` map
  shared observations/results to native state, cancel observations on disposal,
  and keep refresh failures non-blocking when cache exists.
- **Native UI**: SwiftUI provides the list/detail/viewer, `.refreshable`, native
  navigation, system appearance, and Dynamic Type. `AsyncImage` is limited to
  presentation-only image loading; article synchronization and persistence stay
  in shared code.

### Submission country decision

The take-home PDF used `country=id` as a sample. During development it returned
no current articles, so Inosoft was contacted and explicitly approved
`country=us`, currently the only country returning non-empty data. Phase 7C
retains US as the default while also exposing an explicit ID selector and
independent cache; it does not perform automatic country fallback. Empty-state
coverage uses deterministic test data.

## Build & Run

Run these commands from the repository root after configuring `local.properties`:

Prerequisites are an Android SDK with the compile/target SDK configured by the
project, a Java 17–25 launcher, and network access for the first Gradle
bootstrap. The checked-in Gradle daemon configuration targets Java 21. An
attached Android device or emulator is required only for the instrumented
test and manual runtime commands.

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

# Run T4 alone.
./gradlew :androidApp:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=io.mryusuf.kabarkabar.ArticleNavigationTest

# Run T5 alone.
./gradlew :androidApp:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=io.mryusuf.kabarkabar.OfflineBehaviorTest
```

The Android unit task currently runs 5 `ArticleListUiStateTest` cases, 22
`ArticleListViewModelTest` cases, 2 `ArticleDetailViewModelTest` cases, 2
`ThemeTest` cases, and 3 navigation boundary tests (34 total). The
instrumented task runs 17 tests, including T4, T5, Phase 7A state checks, the
Phase 7B viewer matrix, country switching, Room migration, and pagination. The
shared logic suite currently runs 57 tests on Android and 59 on iOS.

To manually verify the current Android application on a connected device:

```bash
adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
adb shell am start -n io.mryusuf.kabarkabar/.MainActivity
```

## Offline-First Decisions

- Room is the exclusive readable source of article data for higher layers.
- Successful remote synchronization writes to Room.
- UI observes local persisted state rather than rendering the raw network response.
- The repository contract is `observeArticles(country)`, `observeArticle(id, country)`, and
  `refreshArticles(country)`, `loadMoreArticles(country)`, and
  `canLoadMore(country)`, where `country` is the shared US/ID model.
- Each article has a stable deterministic local ID derived from its canonical URL; Navigation passes the ID, not the whole article.
- A valid successful empty response may replace the previous snapshot and produces the Empty state.
- Existing cache survives remote refresh failure.
- Malformed/unusable remote data and failed database replacement preserve the previous committed cache.
- A refresh failure with cache is shown as a non-blocking message.
- No cache + no usable remote/local data becomes an explicit error state.

Durable content uses typed Loading, Data, Empty, and Error states. Refreshing is orthogonal to content availability, and one-shot Snackbar/effect messages are not persistent nullable StateFlow data. At the repository boundary, a failed initial refresh is returned as `RefreshResult.Failure` while an empty local snapshot remains `ArticleObservation.Data(emptyList())`; Phase 4 combines those outcomes to derive the blocking Error state, while `RefreshResult.Success` distinguishes a legitimate successful empty snapshot. Compose rendering and navigation are complete, and Phase 5 formally covers the required T4/T5 UI acceptance flows.

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
| 6 | Phase 5 Compose UI acceptance tests (T4 & T5) | See `plan/ai-usage-log.md` Entry 6 | Agent Mode corrections and the subsequent Codex review are explicitly separated; Codex seeded cache before activity launch, recorded the configured refresh failure, and added distinguishable T4 identity fixtures | T4 individually, T5 individually, and together: 2 PASSED on Pixel_9 |
| 7 | Phase 7B Full-Screen Image Viewer | See `plan/ai-usage-log.md` Entry 7 | Agent implementation was independently reviewed; URL double-decoding and unusable-image exposure were corrected, with navigation/back-stack and failure regressions added | Full clean/shared gate plus 13 Pixel_9 connected tests |

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

- [x] system dark mode (Phase 7A),
- [x] full-screen image viewer (Phase 7B),
- [x] country selection and country-aware offline isolation (Phase 7C),
- [ ] commonTest coverage,
- [x] native SwiftUI iOS application (Phase 7E),
- [x] pagination (Phase 7D),
- [ ] full accessibility audit beyond the Phase 7A baseline.

## Known Limitations

- **Offline Images**: Android images depend on Coil's disk cache and the iOS bonus UI uses `AsyncImage`; if an image was not loaded while online, it will not be available offline. Full offline image persistence is out of scope.
- **NewsAPI Content**: NewsAPI typically provides a short description or snippet rather than the full article body. The app displays what is available from the API.
- **Country Availability**: Some countries may return empty results from NewsAPI depending on current news volume or provider availability. The app defaults to `US (country=us)`; `ID (country=id)` remains an independent cache and may show Empty.
- **iOS Test Depth**: The iOS project has two deterministic launch/country UI tests. The richer state and repository matrix remains covered by shared KMP and Android tests; the iOS Simulator manual pass supplements but does not replace those tests.
- **Submission State**: This working tree is an uncommitted candidate. `release-v1.0.0` is the immutable fallback; no Phase 8 work or release tag is authorized by this review.

## Future Work / Bonuses
- **Country Selection Persistence**: Preserve the selected country across launches; 7C currently defaults to US on a new launch.
- **Pagination**: Paging metadata is kept in the shared repository instance and
  is reset by a successful refresh. Both Android and iOS call the shared
  load-more path; iOS uses a near-end row trigger.
- **Accessibility**: Conduct a full WCAG audit and improve screen reader support beyond the Phase 7A baseline.
- **iOS Application**: Broader deterministic iOS detail/pagination-failure UI coverage remains optional future hardening; shared KMP and Android tests cover those behavior matrices today.

## Submission Checklist

- [x] Public GitHub/GitLab repository
- [ ] Candidate clean checkout is runnable after API-key setup (pending final audit)
- [x] No real API key in repository or git history
- [x] Required behavior works
- [x] Required tests pass
- [ ] README setup instructions verified on a clean candidate state
- [x] Architecture and trade-offs documented
- [x] 3+ Android Studio Agent Mode tasks documented
- [x] At least 1 real AI mistake/correction documented
- [x] Known issues stated explicitly

The checklist describes the locally verified candidate. Because the candidate
is not yet pushed, repeat the anonymous fresh-clone check after commit/push
before submitting.
