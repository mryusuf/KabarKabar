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

### T2 - Remote Failure -> Cache Survives

Given persisted articles exist,
when the remote request fails,
then the repository/use case continues exposing cached articles
and reports refresh failure without deleting the cache.

### T3 - Remote + Local Unavailable -> Error

Given there is no usable local data,
when remote loading fails,
then the presentation/domain layer receives a relevant error outcome.

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

## Useful Additional Tests

Only after required tests:

- empty remote result,
- malformed/partial article mapping,
- refresh loading state does not hide cache,
- publication date formatting,
- back navigation,
- DB write failure,
- duplicate/synchronization policy,
- commonTest use-case/business-rule coverage.

## Test Design

Prefer deterministic fakes where possible:

```text
FakeRemoteDataSource
FakeArticleRepository / Fake DAO boundary
Controlled failure/result
Test dispatcher where coroutine timing matters
```

Use MockK/equivalent when interaction-based mocking is clearer than a fake.

UI tests should override DI with deterministic data rather than use the real NewsAPI.

## Regression Rule

When a bug is found in:

- refresh,
- cache fallback,
- mapping,
- navigation,
- error handling,

add or strengthen a test that would have caught it before fixing the implementation.
