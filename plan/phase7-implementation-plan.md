# Phase 7 Implementation Plan

## Status

- Baseline: `release-v1.0.0`
- Baseline meaning: audited Phase 0–6 submission-safe release.
- Phase 7: 7A, 7B, and the reviewed 7C candidate are complete locally; 7D and later remain NOT STARTED.
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

7D remains not started: no page/pageSize, append, load-more, or paging metadata
was added.

# Phase 7D — Pagination / Load More

- [ ] explicit page/pageSize support
- [ ] has-more from response metadata
- [ ] refresh/load-more separate
- [ ] page-1 refresh resets coherently
- [ ] next page appends persisted data
- [ ] deterministic dedupe
- [ ] list visible while loading
- [ ] footer progress
- [ ] failure -> simple Load more button
- [ ] no blocking pagination error
- [ ] duplicate triggers controlled
- [ ] country isolation preserved
- [ ] D1–D6 tests
- [ ] runtime QA
- [ ] Codex 7D gate: `SAFE TO START PHASE 7E: YES`

# Phase 7E — Native iOS UI

- [ ] verify current iOS framework integration
- [ ] native SwiftUI app target/project
- [ ] thin observable/presentation adapter
- [ ] shared repository/data used
- [ ] List + prominent first item concept
- [ ] Loading / Empty / Error
- [ ] pull-to-refresh
- [ ] Detail + native back
- [ ] offline list/detail
- [ ] native dark/light
- [ ] Dynamic Type/accessibility
- [ ] 7B viewer if retained
- [ ] 7C selector if retained
- [ ] 7D load-more if retained
- [ ] iOS tests
- [ ] Simulator QA
- [ ] README iOS setup/limitations
- [ ] Android full regression remains green
- [ ] Codex 7E gate: `SAFE TO START FINAL PHASE 7 AUDIT: YES`

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
