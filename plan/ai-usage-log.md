# Android Studio Agent Mode Usage Log

This log is for **actual Android Studio Agent Mode work only**.

The assessment requires at least 3 meaningful engineering tasks and at least 1 real example where an AI-generated mistake/suboptimal choice is identified and corrected.

Do not fabricate an error just to satisfy the requirement.

---

## Entry 1

**Task/category:**  
Phase 0: Project Realignment and Stack Setup

**Prompt / context given to Agent Mode:**  
Realign the existing KMP scaffold with the accepted target architecture and establish a clean buildable foundation. Tasks included module ownership cleanup (removing sharedUI), updating entry point to a placeholder, adding core dependencies (Ktor, Room, Koin, etc.), and configuring API key handling.

**What the agent produced:**  
Updated `libs.versions.toml`, `settings.gradle.kts`, `androidApp/build.gradle.kts`, `sharedLogic/build.gradle.kts`, and `MainActivity.kt`.

**My review:**  
The agent successfully disconnected the unused module and set up the required dependency stack. It also implemented a safe way to read the API key from `local.properties` without exposing it in the domain layer.

**What I changed or rejected, and why:**  
Identified a suboptimal choice: I initially used hyphenated accessors for some libraries in the build scripts (e.g., `libs.ktor.serialization.kotlinx-json`), which caused compilation errors because hyphens are interpreted as the minus operator. I corrected these to use the standard dot notation (e.g., `libs.ktor.serialization.kotlinx.json`). Also, MockK was temporarily removed from `sharedLogic/commonTest` because of resolution issues with iOS targets, which will be addressed in a later phase.

**Validation performed:**  
The Agent Mode session reported successful `gradle_sync`,
`:androidApp:assembleDebug`, and `:sharedLogic:assemble` builds. The Android
unit-test task was later verified as `NO-SOURCE`, not as a passing test suite;
the shared KMP tests pass. Independent Phase 0 follow-up verification also
covered lint/check and launching the placeholder on the `Pixel_9` emulator.

**Commit / PR (optional):**  
N/A (Phase 0 Baseline)

---

## Entry 2

**Task/category:**  
Phase 2: Data Layer (Infrastructure & Repository)

**Prompt / context given to Agent Mode:**  
Implement the shared remote and local infrastructure (Phase 2A) and the offline-first ArticleRepository (Phase 2B). This includes NewsAPI DTOs, Ktor client with redacted logging, Room KMP database/entities/DAOs, and the synchronization logic that preserves cache on failure.

**What the agent produced:**  
Created `NewsApiClient.kt`, `ArticleEntity.kt`, `ArticleDao.kt`, `AppDatabase.kt`, `ArticleMapper.kt`, `RemoteArticleDataSource.kt`, `LocalArticleDataSource.kt`, and `OfflineFirstArticleRepository.kt`. Implemented repository unit tests.

**My review:**  
The agent established a clean cross-layer infrastructure and correctly implemented the "Room as exclusive read source" invariant. The use of a `Mutex` in the repository ensures concurrent refreshes are handled safely.

**What I changed or rejected, and why:**  
The Phase 2 Agent Mode review identified and corrected the following
suboptimal choices/mistakes before this Codex review:
1. **Mapping Logic Error**: During the Phase 2A safety check, the Agent Mode work identified that `ArticleMapper` initially didn't distinguish between a legitimate empty response and a malformed-data failure. The Phase 2 correction introduced `ArticleMappingResult` so the repository can decide whether to replace the cache or preserve it.
2. **Missing Test Dependencies**: The agent initially forgot to add `kotlinx-coroutines-test` to the project's dependency catalog and build scripts, which caused repository tests to fail. The Phase 2 correction added the library to `libs.versions.toml` and `sharedLogic/build.gradle.kts`.
3. **Room KMP configuration**: The Phase 2 work adjusted the `@ConstructedBy` and `AppDatabaseConstructor` pattern to align with Room 2.7.0+ KMP requirements after identifying generated-source conflicts.

**Validation performed:**  
The Phase 2 Agent Mode checkpoint recorded `./gradlew :sharedLogic:allTests`
with 19 passing tests, confirmed malformed remote data preserves the cache, and
reviewed API-key redaction. The stronger targeted redaction regression test and
the clean end-to-end gate were run during this Codex review and are not claimed
as Agent Mode evidence.

**Commit / PR (optional):**  
N/A (Phase 2 Complete)

---

## Entry 3

**Task/category:**  
Phase 3: Critical Unit/Acceptance Tests (T1-T3)

**Prompt / context given to Agent Mode:**  
Implement the required offline-first acceptance tests T1–T3 from `specs/testing.md`. These tests should exercise the public repository/data boundaries and verify fetch-persist-expose, remote failure cache preservation, and error handling for no-data scenarios.

**What the agent produced:**  
Created `OfflineFirstArticleRepositoryAcceptanceTest.kt` with three integrated test cases (T1, T2, T3) following the Given/When/Then structure.

**My review:**  
The agent correctly identified the overlap with existing Phase 2 unit tests and created dedicated acceptance tests that verify the full data flow through the repository's public API. The tests successfully use deterministic fakes.

**What I changed or rejected, and why:**  
The agent initially made minor compilation errors in the test:
1. Referred to `Article.canonicalUrl` instead of `Article.url`.
2. Passed a single DTO to `ArticleMapper.mapToDomain` instead of a list.
3. Forgot to import `ArticleMappingResult`.
4. Failed to cast `ArticleMappingResult` to `Success` before accessing its members.
The Agent Mode follow-up corrected these after IDE feedback by updating the
property name, passing a list, adding the import, and performing the cast.
These were minor compile-time test-code corrections; no production code was
changed for Entry 3.

**Validation performed:**  
Ran `./gradlew :sharedLogic:allTests` which passed with 38 tests on Android and 39 on iOS (the extra iOS test is an infrastructure-specific check).

**Was this the required mistake/correction example?**  
No, the primary documented mistake remains the Phase 2 ArticleMapper empty-vs-malformed correction.

**Commit / PR (optional):**  
N/A (Phase 3 Complete)

---

## Entry 4

**Task/category:**
Phase 4A: Presentation State Orchestration & ViewModel

**Prompt / context given to Agent Mode:**
Implement the smallest Android-owned presentation layer that converts the existing repository contracts into the approved `ArticleListUiState` semantics. Handle initial load, refresh, and non-blocking errors. Implement unit tests (V1-V5) using a fake repository.

**What the agent produced:**
Created `ArticleListViewModel`, `ArticleListUiEvent`, Koin `AppModule`, `KabarKabarApp` Application class, and `ArticleListViewModelTest`.

**My review:**
The agent correctly implemented the orchestration of `observeArticles()` and `refreshArticles()`. It used `StateFlow` with `combine` to derive the UI state and a `SharedFlow` for one-shot events. The DI setup was added as it was missing in the code.

**What I changed or rejected, and why:**
Identified a semantic mistake: The initial `combine` logic in the ViewModel didn't correctly distinguish between "Initial Loading" and "Blocking Error" when the cache was empty and the initial synchronization failed. The agent initially relied solely on the repository's `ArticleObservation`, but an empty observation is just `Data(emptyList())` and doesn't communicate that a sync failure occurred. I corrected this by introducing a `lastSyncError` StateFlow to track the sync result and using it in the `combine` block to transition to `ArticleListContent.Error` when the cache is empty.

**Subsequent independent review note (not Agent Mode evidence):**
The Phase 4A review found additional semantic defects in the generated orchestration: cache classification depended on the exposed StateFlow's subscription timing, refresh calls could duplicate before coroutine scheduling, default `SharedFlow` delivery could lose a late snackbar collector, and a later local observation failure hid previously displayed data. Codex added deterministic regressions and corrected these behaviors, plus completed the missing application-scoped repository DI graph.

**Validation performed:**
The Agent Mode checkpoint ran `:androidApp:testDebugUnitTest` with 9 tests, including V1-V5, and reported the shared build/check/assemble/lint tasks green. The independent review first reproduced three defects with failing tests, then ran the corrected focused task with 15 passing tests. The final clean verification gate is recorded after completion of this review.

**Commit / PR (optional):**
N/A (Phase 4A reviewed/remediated; Phase 4B pending)

---

## Entry 5

**Task/category:**
Phase 4B: Android Compose UI & Navigation

**Prompt / context given to Agent Mode:**
Implement the Android Compose UI, Navigation, article detail flow, image loading via Coil, and pull-to-refresh. Ensure the UI respects the offline-first invariants and uses the existing `ArticleListViewModel`. Pass only the stable `ArticleId` between screens and resolve data from the repository in the detail screen.

**What the agent produced:**
Created `ArticleListScreen`, `ArticleRow`, `ArticleDetailScreen`, `ArticleDetailViewModel`, `KabarKabarNavGraph`, and `DateFormatter`. Updated `MainActivity` and `AppModule`.

**My review:**
The agent successfully built the UI screens and connected them using Navigation Compose. The pull-to-refresh was correctly bound to the ViewModel. Image loading via Coil was integrated into both list and detail screens.

**What I changed or rejected, and why:**
Identified a real mistake: The initial navigation implementation used `ArticleId` directly in the route (e.g., `article_detail/{articleId}`). Since `ArticleId` contains a full URL with slashes, this broke the navigation graph as the slashes were interpreted as path separators, causing a `java.lang.IllegalArgumentException` at runtime. I corrected this by implementing `URLEncoder` and `URLDecoder` in `KabarKabarNavGraph` to safely pass the ID as a single path segment.

The first Phase 4B image attempt also failed at runtime because Coil 3 does not include network fetching by default and the application-level ImageLoader/network integration was incomplete. The follow-up remediation added `coil-network-ktor3` and configured the application singleton with `KtorNetworkFetcherFactory`. Runtime checks on the Pixel 9 emulator then verified list and detail image loading. This entry records the actual Gemini-assisted debugging/remediation; the independent static/runtime review and any fixes in this review are not attributed to Agent Mode.

The whole-Phase-4 Codex review later added separate regression coverage for
presentation-safe detail errors, stable detail-state collection, and the
Navigation Compose percent-encoded ID boundary. Those changes are independent
review work and are not Agent Mode evidence.

**Validation performed:**
Ran `./gradlew :androidApp:assembleDebug` and performed manual verification on a Pixel 9 emulator (R1-R8). Verified that tapping an article now correctly navigates to the detail screen without crashing, and that back navigation works as expected.

**Commit / PR (optional):**
N/A (Phase 4B Complete)

---

## Entry 6

**Task/category:**
Phase 5: Compose UI Acceptance Tests (T4 & T5)

**Prompt / context given to Agent Mode:**
Implement the required Compose UI acceptance tests T4 and T5 from `specs/testing.md`. T4 verifies the full list-to-detail navigation flow. T5 verifies that cached articles remain visible even when the remote refresh fails. Use a fake repository and a custom TestRunner to ensure deterministic results without hitting the real network.

**What the agent produced:**
Created `KabarKabarTestRunner`, `TestKabarKabarApp`, `FakeArticleRepository`, `ArticleTestData`, `ArticleNavigationTest` (T4), and `OfflineBehaviorTest` (T5). Updated `libs.versions.toml` and `androidApp/build.gradle.kts`.

**Agent Mode checkpoint review:**
The agent established the test infrastructure with a custom `AndroidJUnitRunner`
that bypasses production Koin initialization and a deterministic
`FakeArticleRepository`. The initial checkpoint reported the real `NavHost`
navigation and the intended offline scenario, but the independent review below
found that the T5 setup did not yet prove the refresh failure was observed.

**What I changed or rejected, and why:**
The Agent Mode checkpoint identified a real mistake: the initial
`ArticleTestData` used `ArticleId.fromValue("https://example.com/1")` without
the required `article-url:` prefix, causing an `IllegalArgumentException`
during article construction. That checkpoint corrected it to use
`ArticleId.fromCanonicalUrl(url)`.

The Agent Mode checkpoint also corrected the incompatible `ui-test-manifest`
version and addressed a `NoSuchMethodException` on newer APIs by forcing
Espresso 3.7.0 and Test Runner 1.7.0 via a Gradle `resolutionStrategy`.

**Subsequent Codex review/remediation (not Agent Mode evidence):**
The initial T5 arranged cache emission and the failure result inside the test
method, after `MainActivity` could already have launched and completed its
automatic refresh. T4 also used only one article. Codex changed the fake to
accept initial persisted articles and an initial refresh result, record refresh
calls, seeded both tests before activity launch, used two distinguishable T4
articles, and removed remote image URLs from the fixtures. No production
feature code or test-only production branch was added.

**Validation performed:**
The Agent Mode checkpoint ran `./gradlew :androidApp:connectedDebugAndroidTest`
with 2 tests on the Pixel 9 emulator. Codex then ran T4 individually, T5
individually, and both together on `Pixel_9`, followed by the exact clean
regression gate: `sharedLogic` tests (38 Android, 39 iOS), `androidApp` unit
tests (18 cases), shared assemble/check, Android debug assemble, and lint.

**Commit / PR (optional):**
N/A (Phase 5 Complete)

---

## Entry 7

**Task/category:**
Phase 7B: Full-Screen Image Viewer

**Prompt / context given to Agent Mode:**
Implement a full-screen image viewer that can be opened from the article detail screen hero image. Use standard Compose Navigation, dark background, fit content scale, and a close button. Support system back button. Reuse existing Coil for image loading. Pass image URL via navigation.

**What the agent produced:**
Created `ImageViewerScreen.kt`, updated `NavRoutes`, `KabarKabarNavGraph.kt`, `ArticleDetailScreen.kt`, `strings.xml`, and `ArticleTestData.kt`. Added `ImageViewerTest.kt`.

**My review:**
The agent implemented the new screen and integrated it into the navigation graph. It added URL encoding/decoding for the navigation argument, but the independent Codex review below later found that the destination decoded the already-decoded argument a second time. The hero image in the detail screen was made clickable, and the image viewer provided the required full-screen experience with a dark background and a close affordance.

**What I changed or rejected, and why:**
1. **Wrong File Path**: I initially attempted to update `strings.xml` using an incorrect path (`androidApp/src/res/values/strings.xml` instead of `androidApp/src/main/res/values/strings.xml`). The tool correctly reported the error, and I fixed the path immediately.
2. **Test Data Limitation**: I noticed that the existing `ArticleTestData` only contained articles with `null` image URLs, which would make testing the image viewer impossible. I added a new `articleWithImage` fixture to `ArticleTestData` and updated `ImageViewerTest` to use it, ensuring both the "has image" and "no image" cases are covered.

**Validation performed:**
At the Agent Mode checkpoint, the full gate specified in the phase requirements
was executed: `./gradlew clean :sharedLogic:allTests :sharedLogic:assemble :sharedLogic:check :androidApp:testDebugUnitTest :androidApp:assembleDebug :androidApp:lintDebug :androidApp:connectedDebugAndroidTest`. All tests passed (7 instrumented tests total, including 2 new tests for the image viewer). Manual verification on the emulator confirmed smooth navigation from Detail to Viewer and back via both the close button and the system back button.

**Subsequent Codex 7B adversarial remediation (not Agent Mode evidence):**
The review added a regression for URL values containing `+` and percent escapes,
then removed the second decode at the Navigation boundary. It also rejected
blank/whitespace-only and malformed image URLs before exposing the hero action, added
single-top viewer navigation, replaced the fixed close-button offset with
status-bar insets, removed an unused viewer string, and strengthened the
instrumentation matrix for exact one-layer close, preserved Detail/List routes,
refresh-count stability, and failed-image safety.

The revalidation gate passed with 36 Android shared tests, 37 iOS simulator
shared tests, 21 Android application unit tests, and 12 Pixel_9 connected tests;
`git diff --check` passed. This independent Codex work is not attributed to
Android Studio Agent Mode.

---

## Entry 8

**Task/category:**
Phase 7C: US/ID Country Selection + Country-Aware Offline Correctness

**Prompt / context given to Agent Mode:**
Implement country selection for US (🇺🇸) and ID (🇮🇩) with isolated local caches. This involves database migration to a composite primary key `(id, countryCode)`, updating the `ArticleRepository` contract to be country-aware, and adding a country selector UI. Ensure that switching countries preserves and correctly displays country-specific data offline.

**What the agent produced:**
Updated `ArticleEntity`, `ArticleDao`, `AppDatabase` (migration 1->2), `ArticleRepository` interface, `OfflineFirstArticleRepository`, `ArticleListViewModel`, `ArticleDetailViewModel`, `ArticleListScreen`, `NavRoutes`, and `KabarKabarNavGraph`. Updated and added unit/connected tests.

**My review:**
The agent successfully performed a non-destructive database migration and isolated the caches by country. The UI selector correctly triggers a country switch and restarts the data observation. The persistence of other-country data during a refresh of the selected country was correctly implemented in the DAO and data sources.

**What I changed or rejected, and why:**
1. **Duplicate Method Definitions**: During the ViewModel update, I accidentally left duplicate copies of several private methods at the end of the file because the replacement block wasn't precise enough. I identified the 25 compilation errors immediately and removed the redundant code.
2. **Test Isolation Mistake**: The first attempt at the country-switch unit test failed because the `FakeArticleRepository` in the test used a single `SharedFlow` for all countries. This caused the "ID" observation to immediately receive the "US" articles from the replay buffer. I corrected this by updating the fake repository to use a `Map<String, Flow>` to correctly simulate isolated caches.
3. **Migration Omission**: I initially only added the Room migration to the `androidApp` DI module. I realized this would cause iOS tests to fail (and they did). I corrected this by moving the migration registration into the platform-specific `getDatabaseBuilder()` actual implementations in `sharedLogic`.

**Validation performed:**
Executed the full gate: `./gradlew clean :sharedLogic:allTests :sharedLogic:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:connectedDebugAndroidTest`. All tests passed (14 instrumented tests total, including the new `CountrySelectionTest`). Manual verification confirmed that switching to ID (which NewsAPI currently returns as empty) correctly shows the Empty state without erasing the US cache, and switching back to US immediately restores the cached data.

**Subsequent Codex 7C adversarial remediation (not Agent Mode evidence):**
The persistence review found that the candidate still exposed unconstrained
string country values and that the list ViewModel could let a late refresh from
the previous country block or classify the selected country. Codex replaced the
country boundary with the shared US/ID model, added generation/country guards
around refresh results, added state-based C1–C7 regressions including a real v1
Room migration test, and verified the final candidate with 45 Android shared
tests, 47 iOS simulator shared tests, 26 Android application unit tests, and
15 Pixel_9 connected tests. These corrections are independent of the recorded
Agent Mode checkpoint above.

---

## Entry 9

**Task/category:**
Phase 7D: Pagination / Load More

**Prompt / context given to Agent Mode:**
Implement pagination for the News Reader using NewsAPI's `page` and `pageSize` parameters. Update the `ArticleRepository` to support `loadMoreArticles(country)` and `canLoadMore(country)`. Manage isolated paging state per country in the repository. Update the `ArticleListViewModel` and `ArticleListScreen` to support a non-blocking "Load more" footer and trigger loading at the end of the list. Implement D1-D6 tests.

**What the agent produced:**
Updated `ArticleRepository.kt`, `RemoteArticleDataSource.kt`, `LocalArticleDataSource.kt`, `OfflineFirstArticleRepository.kt`, `ArticleListUiState.kt`, `ArticleListViewModel.kt`, `ArticleListScreen.kt`, and `strings.xml`. Updated and added repository and ViewModel unit tests.

**Initial review recorded at the Agent Mode checkpoint:**
The checkpoint reported country-isolated paging, independent refresh/load-more
state, an end-of-list trigger, and a graceful failure fallback. A later Codex
adversarial review did not accept that checkpoint as final evidence; the RED
tests below found stale-operation and metadata/dedupe defects.

**What I changed or rejected, and why:**
1. **Conflicting Mutex logic**: I initially considered using separate mutexes for refresh and load-more, but realized this could lead to race conditions when modifying the same local database snapshot. I decided to reuse the `refreshMutex` to ensure atomic modifications of a country's article set.
2. **Missing Test Data Helper**: The repository unit tests initially failed to compile because I forgot to include the `articleDto` helper function in the test file when adding the D1-D4 tests. I added the helper to restore the build.
3. **PagingFooter Logic Warning**: The initial `PagingFooter` implementation had a redundant `if (isError)` check that triggered a "condition is always true" lint warning due to the preceding guard clause. I simplified the logic to use a `when` block for better clarity and to satisfy the linter.

**Validation reported for the Agent Mode checkpoint:**
The entry reported the full gate command and simulated UI checks. That report is
retained as historical Agent Mode evidence, not as the final 7D approval.

---

## Codex 7D adversarial remediation

The follow-up review wrote failing tests before changing production behavior. It
reproduced stale page results after refresh and country switch, duplicate
load-more triggers, page failure/cache preservation, invalid page metadata, and
cross-page duplicate replacement. The remediation added repository generation
checks, per-country mutexes, transactional conflict-ignore append, ViewModel
page-job tokens, and deterministic Pixel_9 pagination tests.

Final local validation passed with 56 Android shared tests, 58 iOS simulator
tests, 34 Android application unit tests, and 17 Pixel_9 connected tests. This
Codex work is separate from the Android Studio Agent Mode evidence above; it did
not add a new Agent Mode task.

---

---

## Entry 10

**Task/category:**
Phase 7E: Native SwiftUI iOS Application

**Prompt / context given to Agent Mode:**
Implement a native SwiftUI iOS application that reuses the KMP shared logic. Refactor the existing Koin DI to move shared components from `androidApp` to `sharedLogic/commonMain`. Implement a `FlowWrapper` in `sharedLogic/iosMain` to facilitate Flow consumption in Swift. Create native SwiftUI ViewModels and Views in `iosApp` that mirror the Android functionality (List with refresh/pagination, and Detail). Ensure the iOS app follows native design principles and supports light/dark mode.

**What the agent produced:**
- `SharedModule.kt`: Moved repository, database, and networking DI to `commonMain`.
- `FlowWrapper.kt` & `KoinIOS.kt`: Added Swift ergonomics to `iosMain`.
- Updated `androidApp` DI to use `sharedModule`.
- `Koin.swift`, `ArticleListViewModel.swift`, `ArticleDetailViewModel.swift`, `ArticleListView.swift`, `ArticleDetailView.swift`, and `iOSApp.swift` in `iosApp`.
- Updated `README.md` with iOS instructions.

**Checkpoint review (not accepted as final evidence):**
The checkpoint described a reusable shared module and a Swift bridge, but its
conclusions were not accepted without compilation and lifecycle verification.
The candidate still had a real compile error (`SyncError` has no `message`),
referenced fields absent from the shared `Article` model, ignored
`RefreshResult.Failure`, hard-coded US detail navigation, and retained the
Swift observation callback strongly enough to risk a ViewModel/collector
cycle. These are concrete defects in the generated candidate, not invented
examples.

**What I changed or rejected, and why:**
1. **Unused Imports in Android DI**: After moving components to `sharedModule`, I initially left several unused imports in `androidApp/AppModule.kt`. I cleaned these up after the agent pointed them out via warnings.
2. **Swift Generic Loss**: I noticed that `ArticleObservation<T>` might lose its generic type when bridged to Swift through `FlowWrapper`. I used explicit casting in Swift (`observation as? ArticleObservationData<NSArray>`) to ensure type safety.
3. **AsyncImage Placeholder**: The initial SwiftUI detail view didn't have a placeholder for images. I added a simple colored rectangle to improve the UI while images are loading.

**Validation reported at the checkpoint:**
The entry originally reported shared tests, Android tests, a simulator framework
link, and manual iOS flows. Those claims are retained as historical Agent Mode
output only; the later Codex audit did not treat them as verified evidence.

## Codex 7E adversarial remediation

The follow-up review corrected the Swift state mapping and model-field errors,
made refresh results and cache-preserving failures explicit, passed the shared
repository and country to detail, added weak callbacks and cancellable scope
ownership, removed the unnecessary Koin framework export, moved Android DI to an
Android-only bridge, added ignored iOS xcconfig setup, and added a deterministic
iOS launch UI-test target/scheme. The iOS test target is intentionally small;
shared KMP and Android tests remain the deeper behavior matrix.

Independent final local evidence:

- clean shared/Android gate: 56 Android shared tests, 58 iOS shared tests, 34 Android unit tests, lint/check/assemble green;
- Pixel_9 connected suite: 17/17 passed;
- Apple framework links: simulator and device debug frameworks passed;
- Xcode `iosApp` build passed;
- Xcode `iosAppTests` on iPhone 17 Pro Max iOS Simulator: 1/1 passed;
- Computer Use Simulator QA observed list/prominent row, detail/back, viewer,
  US/ID switching and cache isolation, cached refresh failure, light/dark,
  and Dynamic Type. Pull-to-refresh is implemented through SwiftUI
  `.refreshable`; the visual gesture was not used as deterministic test evidence.

## Evidence status

Entries 1–10 are recorded above. Entry 2, Entry 3, Entry 6, Entry 8, Entry 9, and Entry 10 are
meaningful Android Studio Agent Mode tasks. Entry 9 is an historical checkpoint;
the subsequent Codex review and remediation are documented separately and are
not presented as Agent Mode evidence.

## Good Candidate Tasks

Choose tasks that genuinely happen during implementation, for example:

- review the repository/offline-first design against `specs/architecture.md`,
- generate or strengthen tests from `specs/testing.md`,
- refactor a concrete code smell after the first working version,
- debug a real Room/KMP, Ktor, coroutine, or Compose issue,
- review error-handling paths for a missing case.

A task is stronger when the log shows engineering judgment rather than "generate the whole app."
