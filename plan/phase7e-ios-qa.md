# Phase 7E iOS QA Record

Date: 20 August 2026
Candidate: uncommitted working tree; immutable fallback `release-v1.0.0` was not modified
Simulator: iPhone 17 Pro Max, iOS 26.3.1, `5D01A82B-029F-4726-BC8C-CB2CC1B93AE4`

## Build and test evidence

- `./gradlew clean :sharedLogic:allTests :sharedLogic:assemble :sharedLogic:check :androidApp:testDebugUnitTest :androidApp:assembleDebug :androidApp:lintDebug` — passed.
- `./gradlew :androidApp:connectedDebugAndroidTest` — 17/17 Pixel_9 tests passed.
- `./gradlew :sharedLogic:linkDebugFrameworkIosArm64 :sharedLogic:linkDebugFrameworkIosSimulatorArm64` — passed.
- `xcodebuild ... -scheme iosApp ... build` — passed.
- `xcodebuild ... -scheme iosAppTests ... test` — 1/1 passed with the `--ui-test-no-network` launch fixture; result bundle `/tmp/kabarkabar-phase7e-deterministic-test.xcresult`.

The original `iosApp` user scheme had no test action; the candidate now contains
the reproducible shared `iosAppTests` scheme and `iosAppUITests` target.

## Feature matrix

| Case | Result | Evidence |
|---|---|---|
| List | PASS | Computer Use screenshot and accessibility inspection with live US cache |
| Prominent first item | PASS | First row rendered with hero image; later rows compact |
| Loading / Empty / Error | PASS | Source state mapping; blank-key no-cache Error; ID successful-empty state |
| Pull-to-refresh | PASS* | SwiftUI `.refreshable` awaits shared refresh; compiled and Android regression is green |
| Cached refresh failure non-blocking | PASS | Blank-key build preserved US rows and showed banner |
| Detail | PASS | Computer Use opened cached article detail |
| Back navigation | PASS | Native back returned to the same list |
| Offline list | PASS | Cached list remained visible after refresh failure |
| Offline detail | PASS | Cached detail opened after failed refresh |
| System light/dark | PASS | Simulator light and dark list/detail screenshots |
| Dynamic Type/basic accessibility | PASS | Accessibility tree plus extra-extra-large content-size detail inspection |
| Full-screen viewer | PASS | Open/Done flow observed with Computer Use |
| US/ID selector | PASS | US -> ID Empty -> US restored cached rows |
| Country cache isolation | PASS | ID empty state did not clear US cache; shared repository owns scope |
| Pagination/load-more | PASS (shared) | Shared repository/Android 17-test evidence; iOS calls shared paging API, but live iOS dataset did not expose a footer during this run |

`*` The iOS refresh implementation is verified by source, compilation, and the
shared/Android behavior gate. A Simulator drag was attempted, but its transient
refresh indicator was not accepted as standalone deterministic evidence.

## Lifecycle review

- Flow collection is created in `FlowWrapper` on `Dispatchers.Main` and cancelled
  through a scope-owned `Cancellable`.
- Swift observation closures capture ViewModels weakly.
- List country changes cancel the prior observation and sync/page tasks and use
  an operation generation before applying late results.
- Detail retry cancels the prior observation before creating a new one.
- Cancellation is not mapped to a user-facing failure.

## Manual QA method

The separate manual stage used Computer Use against the visible Simulator window;
MCP/CLI launch/install actions were not counted as manual UI actions. Evidence
was re-observed after each navigation/action. No Phase 8 or unrelated module work
was performed.

## Feedback remediation — 20 August 2026

- The reported cached-list banner was traced to a missing ignored
  `Config.local.xcconfig`: an empty `NEWS_API_KEY` caused the initial Ktor sync to
  return `SyncError.RemoteApi` while Room still emitted cached articles. iOS now
  runs cache-only until a non-empty local key is configured, so missing setup does
  not present a misleading service-failure banner. A deliberately invalid
  non-empty key was also checked; genuine remote failures still remain visible
  and non-blocking with cache.
- Later rows now render a fixed 112x84 `AsyncImage` thumbnail on the left when the
  shared article contains an image URL. Rows with no image URL remain text-only.
- iOS load-more triggering now starts within the final four visible articles,
  while the repository, metadata, and deduplication remain shared. The existing
  17-test Pixel_9 pagination gate remains green.
- Country-switch regression coverage now selects Indonesia in no-network mode and
  verifies that an empty ID cache is presented as ordinary Empty state, not as an
  API-key message. The iOS UI suite passes 2/2.
- Verification after remediation: Xcode app build passed, `iosAppTests` passed
  2/2, the full Gradle clean gate passed, and Pixel_9 connected tests passed
  17/17.
