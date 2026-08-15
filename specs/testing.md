# Testing Specification

## Principle

Test behavior that demonstrates the architecture works, especially offline-first behavior.

Avoid tests that only assert internal method calls or inflate coverage.

## Required Unit/Shared Tests

### T1 - Fetch -> Map -> Persist -> Expose

Given the remote source returns articles,
when refresh runs,
then valid articles are mapped,
persisted,
and become observable from the local source/repository.

The test verifies observable persisted-data behavior, not only internal method calls. A fake remote source and deterministic local boundary are appropriate for this repository test; Room construction remains isolated behind its own production seam.

### T2 - Remote Failure -> Cache Survives

Given persisted articles exist,
when the remote request fails,
then the repository/use case continues exposing cached articles
and reports refresh failure without deleting the cache.

The cached data must remain the same before and after the failed refresh.

### T3 - Remote + Local Unavailable -> Error

Given there is no usable local data,
when remote loading fails,
then the presentation/domain layer receives a relevant error outcome.

This must be distinct from the deliberate `Empty` state.

## Required UI Tests

### T4 - List -> Detail Navigation

Given article data is available,
when the app opens,
then the list is shown;
when an article is tapped,
then the detail screen shows that article.

### T5 - Cached/Offline Render

Given previously stored/fake cached articles and a failing/unavailable network,
when the list screen opens,
then cached articles remain renderable.

The UI test must use deterministic injected data and must not call the real NewsAPI.

## Cheap High-Value Additions

These additions are valuable but must not become implementation blockers:

- cache remains visible during refresh,
- valid successful empty result produces `Empty`,
- failed transactional database replacement preserves old cache,
- stable ID derivation and duplicate behavior.

## Test Design

Prefer deterministic fakes where possible:

```text
FakeRemoteDataSource
FakeLocalArticleDataSource / local boundary
FakeArticleRepository for presentation tests
Controlled failure/result
Test dispatcher where coroutine timing matters
```

Construct the repository and ViewModel with injected boundaries so tests do not depend on global DI state. Use MockK/equivalent only when interaction-based mocking is clearer than a fake.

UI tests should override the repository/data boundary with deterministic data rather than use the real NewsAPI. Navigation tests pass the stable article ID and verify detail lookup by ID.

The implementation must expose enough seams to test Room construction separately from repository behavior, without adding another production architecture layer.

## Regression Rule

When a bug is found in:

- refresh,
- cache fallback,
- mapping,
- navigation,
- error handling,

add or strengthen a test that would have caught it before fixing the implementation.
