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

## Evidence status

Entries 1–3 are recorded above. Entry 2 is a meaningful Android Studio Agent
Mode data-layer task and includes the documented empty-vs-malformed mapper
correction, dependency correction, and Room constructor correction. Entry 3 is
the reported meaningful Android Studio Agent Mode acceptance-test task; Codex
review/remediation work is not counted as Agent Mode evidence.

The mapper correction is supported by the Phase 2 artifact available at the
start of this Codex review: the earlier indexed mapper returned a plain article
list, while the pre-review working-tree version introduced the explicit
success/malformed result needed to preserve the empty-vs-malformed distinction.
No additional AI mistake is being claimed here.

## Good Candidate Tasks

Choose tasks that genuinely happen during implementation, for example:

- review the repository/offline-first design against `specs/architecture.md`,
- generate or strengthen tests from `specs/testing.md`,
- refactor a concrete code smell after the first working version,
- debug a real Room/KMP, Ktor, coroutine, or Compose issue,
- review error-handling paths for a missing case.

A task is stronger when the log shows engineering judgment rather than "generate the whole app."
