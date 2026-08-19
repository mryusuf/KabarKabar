# Phase 7 Implementation Plan

## Status

- Baseline: `release-v1.0.0`
- Baseline meaning: audited Phase 0–6 submission-safe release.
- Phase 7: 7A complete locally; 7B and later remain NOT STARTED.
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
through deterministic real-`MainActivity` instrumentation tests. Phase 7B and
all later bonus work remain untouched.

# Phase 7B — Full-Screen Image Viewer

- [ ] tappable hero only with image
- [ ] full-screen viewer
- [ ] close affordance
- [ ] system back
- [ ] no extra data fetch
- [ ] missing-image safe
- [ ] optional zoom only if low risk
- [ ] regression tests
- [ ] runtime QA
- [ ] Codex 7B gate: `SAFE TO START PHASE 7C: YES`

# Phase 7C — Country Selection & Offline Correctness

- [ ] shared US/us and ID/id representation
- [ ] `🇺🇸 US` / `🇮🇩 ID` selector
- [ ] explicit country-scoped repository behavior
- [ ] country represented in persistence ownership
- [ ] non-destructive Room migration
- [ ] old cache preserved as US
- [ ] country-scoped list/detail/refresh
- [ ] cached switch immediate
- [ ] empty/failure isolated by country
- [ ] no hidden mutable repository country
- [ ] country isolation tests C1–C7
- [ ] runtime QA
- [ ] README trade-off update
- [ ] Codex 7C gate: `SAFE TO START PHASE 7D: YES`

Decision: selected-country persistence is optional. If absent, default US and document it.

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
