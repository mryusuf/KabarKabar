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
TBD

**Prompt / context given to Agent Mode:**  
TBD

**What the agent produced:**  
TBD

**My review:**  
TBD

**What I changed or rejected, and why:**  
TBD

**Validation performed:**  
TBD

**Commit / PR (optional):**  
TBD

---

## Entry 3

**Task/category:**  
TBD

**Prompt / context given to Agent Mode:**  
TBD

**What the agent produced:**  
TBD

**My review:**  
TBD

**What I changed or rejected, and why:**  
TBD

**Validation performed:**  
TBD

**Was this the required mistake/correction example?**  
TBD

**Commit / PR (optional):**  
TBD

---

## Evidence status

Only Entry 1 has been recorded so far. Entries 2 and 3 intentionally remain
pending until those meaningful Android Studio Agent Mode tasks actually happen;
Codex work is not counted as Agent Mode evidence.

## Good Candidate Tasks

Choose tasks that genuinely happen during implementation, for example:

- review the repository/offline-first design against `specs/architecture.md`,
- generate or strengthen tests from `specs/testing.md`,
- refactor a concrete code smell after the first working version,
- debug a real Room/KMP, Ktor, coroutine, or Compose issue,
- review error-handling paths for a missing case.

A task is stronger when the log shows engineering judgment rather than "generate the whole app."
