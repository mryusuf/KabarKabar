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

- `commonMain` owns domain models, use cases, repository contracts, business rules, and shared utilities.
- Shared data code owns API models, mapping, repository implementation, networking, and Room persistence wherever practical.
- `androidMain` contains Android-specific code only when a platform API requires it.
- `androidApp` owns Compose UI, Android ViewModels, navigation, and presentation wiring.
- Domain code must not depend on presentation or infrastructure.
- UI must never call Ktor, Room DAOs, or raw data sources directly.
- Do not create God ViewModels, repositories, Activities, or Composables.
- Prefer explicit, small abstractions over speculative framework layers.

## Offline-First Invariants

Room is the source of truth for displayed article data.

Expected flow:

`UI observes Room -> repository syncs remote -> successful response is persisted -> Room emits new data -> UI updates`

Rules:

- Cached content remains visible during refresh.
- Refresh failure with cache is non-blocking.
- No cache + failed remote/local load produces a meaningful error state.
- Remote failure must never erase a usable cache.
- Empty successful data is distinct from failure.
- Map infrastructure errors into clear domain-level outcomes.

## Scope Decision

The PDF lists pull-to-refresh both as expected list behavior and as a bonus. Treat it as **required** to remove ambiguity.

Do not implement these until core requirements and tests pass:

- pagination/load-more,
- full-screen image viewer,
- extra feature modules,
- substantial UI animation/polish,
- iOS UI application.

A shared iOS KMP target is useful if it stays low-risk, but the required deliverable is the Android application.

## Testing Rules

Required behavior is defined in `specs/testing.md`.

Tests should verify observable behavior, not implementation details.

Prefer fakes for deterministic repository/data-flow tests when practical. Use mocks only where they improve clarity.

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
