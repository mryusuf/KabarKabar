# Phase 7 Implementation Plan

## Status

- Baseline: `release-v1.0.0`
- Baseline meaning: audited Phase 0–6 submission-safe release.
- Phase 7: 7A, 7B, 7C, and 7D are locally verified; 7E has a reviewed local candidate with the final Phase 7 audit still pending.
- Final target: consider `release-v1.1.0` only after whole-Phase-7 audit and clean-clone verification.
- Phase 8 modularization: deferred until after submission.

## Stop rule

Every bonus must justify itself independently. If a bonus decreases reliability, clarity, architecture quality, evaluator usability, or test determinism, drop it and return to the last green checkpoint.

# Phase 7A — Visual Polish

- [x] system dark mode
- [x] coherent light theme
- [x] prominent first item, no Featured label
- [x] compact remaining rows
- [x] polished Detail
- [x] polished Loading / Empty / Error
- [x] preserve cached refresh error behavior
- [x] accessibility semantics pass
- [x] larger-font smoke check
- [x] light/dark runtime QA
- [x] README bonus update
- [x] Agent Mode log if used (no new Agent Mode work was performed)
- [x] T1–T5/regression gate
- [x] Codex 7A gate: `SAFE TO START PHASE 7B: YES`

## Phase 7A Codex review verification — 19 August 2026

The immutable `release-v1.0.0` fallback remains unchanged at commit
`afe775cc1e938ccb89ad715e8b6e8f285467bf92`. The remediation candidate changes
only `androidApp` presentation/resources/tests; `sharedLogic` and the Phase 0–6
data/domain contracts were not changed.

The requested clean gate passed with zero failures:

- shared Android host tests: 36
- shared iOS simulator tests: 37
- Android debug unit tests: 20
- Pixel_9 connected UI tests: 5
- shared assemble/check, Android debug assemble, Android lint, and `git diff --check`

Pixel_9 runtime checks covered light/dark List and Detail, first/later navigation,
pull-to-refresh, cached offline refresh failure, blocking no-cache error, and a
larger-font smoke check. Loading, Empty, and blocking Error were also exercised
through deterministic real-`MainActivity` instrumentation tests. At this Phase
7A checkpoint, Phase 7B and all later bonus work remained untouched.

## Phase 7A image follow-up — 19 August 2026

Runtime review found that the prominent first row could reserve hero space while
its remote image was loading or unavailable, leaving a visually empty surface.
`ProminentArticleRow` now trims image input, renders a themed local fallback for
missing/blank URLs, and supplies that fallback during Coil loading/failure. The
deterministic `ArticleNavigationTest` regression covers the missing-image first
item. The 7B viewer, singleton `ImageLoader`, navigation, and shared data layer
remain unchanged.

# Phase 7B — Full-Screen Image Viewer

- [x] tappable hero only with a usable image URL
- [x] full-screen viewer
- [x] close affordance
- [x] system back
- [x] no extra article/data-layer fetch
- [x] missing/blank-image safe
- [ ] optional zoom only if low risk
- [x] regression tests
- [x] runtime QA
- [x] Codex 7B gate: `SAFE TO START PHASE 7C: YES`

## Phase 7B Codex adversarial review — 19 August 2026

The initial candidate double-decoded Navigation's already-decoded image URL
argument, which corrupted literal `+` and percent-encoded URL components. It
also treated whitespace-only image URLs as usable. The candidate was tightened
without changing `sharedLogic`, Room, API behavior, or Article identity:

- Navigation now preserves the single decoded URL boundary and uses
  `launchSingleTop` for repeated viewer opens.
- Detail trims and rejects blank image URLs before exposing the viewer action.
- The close control uses status-bar insets rather than a fixed top offset.
- Focused tests cover encoded URL preservation, close/system-back stack depth,
  no/blank/malformed/failed images, refresh-count stability, and post-close
  restoration.

The full clean Android/shared gate passed with 36 Android shared tests, 37 iOS
simulator shared tests, 21 Android application unit tests, and 13 Pixel_9
connected tests. `git diff --check` passed. Phase 7C and later remain untouched.

# Phase 7C — Country Selection & Offline Correctness

- [x] shared US/us and ID/id representation
- [x] compact accessible `🇺🇸 US` / `🇮🇩 ID` dropdown selector
- [x] explicit country-scoped repository behavior
- [x] country represented in persistence ownership
- [x] non-destructive Room migration
- [x] old cache preserved as US
- [x] country-scoped list/detail/refresh
- [x] cached switch immediate
- [x] empty/failure isolated by country
- [x] no hidden mutable repository country
- [x] country isolation tests C1–C7
- [x] runtime QA
- [x] README trade-off update
- [x] Codex 7C gate: `SAFE TO START PHASE 7D: YES`

Decision: selected-country persistence is optional. If absent, default US and document it.

## Phase 7C Codex adversarial review — 19 August 2026

The candidate was reviewed against the country-selector -> ViewModel -> explicit
repository -> NewsAPI -> Room -> scoped observation -> detail path. The initial
candidate used unconstrained `String` country values and allowed an in-flight
refresh from the previous country to block the new refresh or apply its result to
the selected country's state. Both were corrected without changing the 7D
remote/paging boundary.

The focused and regression evidence is:

- C1–C4 repository state tests cover scoped list/detail reads, selected-country
  replacement, empty snapshots, and failures preserving the other cache.
- C5–C6 ViewModel tests cover immediate cached switching, pending Loading,
  selected-country Empty/Error, and both directions of late in-flight refresh.
- C7 opens a handcrafted v1 Room database on Pixel_9 and verifies the old row
  survives as US while ID remains empty.
- The final clean gate passed with 45 Android shared tests, 47 iOS simulator
  shared tests, 26 Android application unit tests, shared assemble/check,
  Android debug assemble, and Android lint. The full Pixel_9 connected suite
  passed 15 tests with 0 failures, and `git diff --check` passed. A fresh
  anonymous clone remains a post-commit submission gate because this candidate
  is intentionally still uncommitted.

# Phase 7D — Pagination / Load More

- [x] explicit page/pageSize support
- [x] has-more from response metadata
- [x] refresh/load-more separate
- [x] page-1 refresh resets coherently
- [x] next page appends persisted data
- [x] deterministic dedupe
- [x] list visible while loading
- [x] footer progress
- [x] failure -> simple Load more button
- [x] no blocking pagination error
- [x] duplicate triggers controlled
- [x] country isolation preserved
- [x] D1–D6 tests plus adversarial race regressions
- [x] runtime QA
- [x] Codex 7D gate: `SAFE TO START PHASE 7E: YES`

## Phase 7D Codex adversarial review and remediation — 19 August 2026

The initial pagination candidate was not accepted unchanged. Focused RED tests
found stale page completions leaking paging error state after refresh/country
switch, page metadata accepting a negative total, first-seen duplicate content
being replaced by a later page, and unknown paging state defaulting to loadable.

The remediation now:

- sends page 1/page size 20 and validates status, metadata, and articles before
  persistence;
- keeps paging state and operation generations scoped per country, with a
  per-country mutex and stale-response check before append;
- appends inside an explicit Room transaction using conflict-ignore semantics,
  preserving the first committed row for duplicate ArticleIds;
- cancels and tokenizes ViewModel page jobs on refresh/country changes, while
  keeping cached rows visible and exposing only a non-blocking footer failure;
- provides deterministic Pixel_9 append and page-failure footer coverage.

The clean gate passed with zero failures: 56 Android shared tests, 58 iOS
simulator shared tests, 34 Android application unit tests, shared assemble/check,
Android debug assemble, and Android lint. The full Pixel_9 connected suite passed
17 tests with 0 failures, including the new pagination tests. `git diff --check`
passed. The candidate remains intentionally uncommitted; commit/push and a final
anonymous clone remain submission gates. Phase 7E was not started.

# Phase 7E — Native iOS UI

- [x] verify current iOS framework integration
- [x] native SwiftUI app target/project
- [x] thin observable/presentation adapter
- [x] shared repository/data used
- [x] List + prominent first item concept
- [x] Loading / Empty / Error
- [x] pull-to-refresh implementation
- [x] Detail + native back
- [x] offline list/detail and cache-preserving refresh failure
- [x] native dark/light
- [x] Dynamic Type/accessibility baseline
- [x] 7B viewer retained
- [x] 7C selector retained
- [x] 7D load-more calls retained
- [x] iOS launch UI test target and shared test scheme
- [x] Simulator QA
- [x] README iOS setup/limitations
- [x] Android full regression remains green
- [x] Codex 7E gate: `SAFE TO START FINAL PHASE 7 AUDIT: YES` (candidate is not release-approved)
- [ ] final Phase 7 audit: clean clone, history/secret scan, candidate commit/push, and release decision

## Phase 7E Codex adversarial review and remediation — 20 August 2026

The initial candidate was not accepted from its Agent Mode summary. Xcode first
failed on a nonexistent `SyncError.message`; static review also found invalid
Swift `Article` fields, ignored `RefreshResult.Failure`, hard-coded US detail
navigation, and a Flow bridge whose strong callback/self ownership could retain
the collector after disposal. The candidate also lacked an Xcode test action.

The remediation keeps SwiftUI presentation-only: the shared repository remains
the sole article synchronization/persistence/paging boundary, `FlowWrapper`
owns a cancellable main-dispatcher scope, and Swift callbacks are weak. Shared
Koin is no longer exported transitively into the Apple framework; Android gets
an Android-only composition bridge. iOS API-key setup is an ignored xcconfig,
and `iosAppTests` provides a reproducible launch test.

Local evidence:

- shared/Android clean gate: 56 Android shared, 58 iOS shared, 34 Android unit,
  lint/check/assemble all green;
- Pixel_9 connected gate: 17/17 passed;
- iOS framework device/simulator links and Xcode app build: passed;
- `iosAppTests` on iPhone 17 Pro Max / iOS 26.3.1: 1/1 passed;
- Computer Use Simulator QA: list/prominent first item, detail/back, viewer,
  US/ID cache isolation, cached refresh failure, light/dark, and Dynamic Type
  observed. The iOS `.refreshable` implementation remains covered by source
  and compilation; the manual gesture was not used as deterministic evidence.

The candidate remains uncommitted and the immutable `release-v1.0.0` fallback
is unchanged. No Phase 8/module work was started.

## Phase 7E feedback remediation — 20 August 2026

The reported iOS banner was independently reproduced. The checkout had no
ignored `Config.local.xcconfig`, so the Info.plist key expanded to blank; Ktor
returned a remote API failure while Room correctly exposed the existing cache.
The iOS composition root now disables remote synchronization until a non-empty
local key exists, preventing a misleading cached-list banner while preserving
the genuine non-blocking banner for configured-but-failing remote services.

Later SwiftUI rows now load available image URLs into 112x84 left thumbnails,
and the list asks the shared repository for the next page from the final four
rows rather than only the exact last row. No Swift-side paging, persistence, or
deduplication was added.

Post-remediation evidence: Xcode app build passed, `iosAppTests` passed 1/1,
the full Gradle clean gate passed, and the Pixel_9 connected suite passed 17/17.

The follow-up country-switch regression reproduced the no-network ID empty-cache
path. The UI test initially failed because the Empty state exposed the API-key
setup copy; it now presents ordinary country-scoped Empty state and passes 2/2
on the iPhone 17 Pro Max simulator. Shared country identity and repository
behavior were not changed.

# Whole Phase 7 final gate

- [ ] decide retained bonuses
- [ ] revert any bonus that weakens submission
- [ ] full Android gate
- [ ] full iOS gate if retained
- [ ] Android emulator QA
- [ ] iOS simulator QA
- [ ] clean-clone verification
- [ ] secret/history audit
- [ ] README bonus table accurate
- [ ] known issues accurate
- [ ] AI log accurate
- [ ] public remote updated
- [ ] final anonymous clone
- [ ] only then consider `release-v1.1.0`
