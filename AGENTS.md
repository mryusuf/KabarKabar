# AGENTS.md

## Mission

Build a small, production-minded **offline-first News Reader** for the Inosoft Mobile Developer take-home test.

Optimize for:
1. correctness,
2. meaningful KMP sharing,
3. offline behavior,
4. testability,
5. clear engineering decisions,
6. a polished small scope.

Do not chase bonus features until all core acceptance scenarios are green.

## Read First

Before changing code, read:

- `specs/requirements.md`
- `specs/architecture.md`
- `specs/testing.md`
- `plan/implementation-plan.md`

After meaningful work, update the plan and any relevant documentation.

## Non-Negotiable Stack

- Kotlin
- Kotlin Multiplatform (KMP)
- Clean Architecture: Presentation / Domain / Data
- Jetpack Compose
- ViewModel + StateFlow
- Navigation Compose
- Ktor Client with logging + timeouts
- Room for KMP
- Koin or another KMP-compatible DI approach
- Coil
- Gradle Kotlin DSL
- JUnit + MockK or equivalent KMP-compatible test doubles
- Compose UI Testing
- Android Studio Agent Mode for at least 3 meaningful engineering tasks

## Architecture Invariants

- `sharedLogic/commonMain` owns domain models, repository contracts, business rules, shared utilities, and only meaningful use cases.
- `sharedLogic/commonMain` owns API models, mapping, repository implementation, shared Ktor behavior, and Room entities/DAOs/database declarations wherever supported.
- `sharedLogic/androidMain` / `sharedLogic/iosMain` contain only platform-specific engine and database construction required by those targets.
- `androidApp` owns Compose screens, Android ViewModels, Navigation, durable UI state, one-shot UI effects, Coil, and application/composition wiring.
- The existing `sharedUI` module is not part of the target architecture. Do not place new production presentation code there.
- Domain code must not depend on presentation or infrastructure.
- UI must never call Ktor, Room DAOs, or raw data sources directly.
- Do not create God ViewModels, repositories, Activities, or Composables.
- Prefer explicit, small abstractions over speculative framework layers.

## Offline-First Invariants

Room is the exclusive readable source of article data for higher layers.
Network responses never feed UI directly; UI observes persisted data.

Expected flow:

`UI observes repository-backed Room data -> repository fetches remote -> validates/maps the full response -> transactionally replaces the snapshot -> Room emits -> UI updates`

Rules:

- Cached content remains visible during refresh.
- Refresh failure with cache is non-blocking.
- No cache + failed remote/local load produces a meaningful error state.
- Remote failure must never erase a usable cache.
- Malformed/unusable remote data must never erase a usable cache.
- A failed database replacement must leave the previous committed cache intact.
- Empty successful data is distinct from failure.
- Map infrastructure errors into clear domain-level outcomes.
- Use a stable deterministic local article ID derived from the canonical article URL; preserve the canonical URL separately.
- Navigation passes the stable ID, not the whole `Article`.

## Scope Decision

The PDF lists pull-to-refresh both as expected list behavior and as a bonus. Treat it as **required** to remove ambiguity.

Do not implement these until core requirements and tests pass:

- pagination/load-more,
- full-screen image viewer,
- extra feature modules,
- substantial UI animation/polish,
- iOS UI application.

A shared iOS KMP target is useful only if it stays low-risk and its platform construction is complete. An iOS UI is not core scope, and the required deliverable is the Android application.

## Testing Rules

Required behavior is defined in `specs/testing.md`.

Tests should verify observable behavior, not implementation details.

Prefer fakes for deterministic repository/data-flow tests when practical. Use mocks only where they improve clarity.

Required scenarios are T1-T5 in `specs/testing.md`. The documented cheap additions are useful regression coverage but must not become implementation blockers.

Every bug fix affecting critical behavior should add or strengthen a regression test.

## Agentic AI Rules

The take-home specifically requires Android Studio Agent Mode.

- Log only tasks actually performed using Android Studio Agent Mode.
- At least 3 tasks must be meaningful engineering work.
- Record: prompt/task, output, review, changes/rejections, and validation.
- At least 1 logged example must show an AI mistake or suboptimal choice that was corrected.
- Never invent an AI mistake for documentation.
- Never accept generated concurrency, persistence, networking, security, or error-handling code without review.
- This repository-planning assistance does **not** count as the required Android Studio Agent Mode evidence.

Use `plan/ai-usage-log.md` as the working log.

## Secrets

- Never commit the real NewsAPI key.
- Keep the key in a local-only property/configuration file.
- Ensure the local secret file is explicitly ignored by Git.
- Commit only an example file and README instructions.
- Before submission, inspect git history as well as the working tree for leaked secrets.

## Definition of Done

Core is done only when:

- project builds from a clean checkout after local API-key setup,
- article list and detail flows work,
- first online fetch persists articles,
- cached data displays immediately on subsequent launch,
- offline/cache fallback works,
- refresh failure preserves cache and surfaces a non-blocking message,
- empty and unrecoverable error states are clear,
- required unit tests pass,
- required UI tests pass,
- README documents setup, architecture, decisions, tests, AI usage, known issues, and bonus scope,
- no secrets are committed.
