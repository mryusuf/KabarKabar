# Architecture Specification

## Goals

The architecture should make offline behavior easy to reason about, keep business/data logic shared, and keep Android-specific presentation isolated.

## Dependency Direction

```text
androidApp/presentation -----> shared/domain
                                   ^
                                   |
shared/data ----------------------+
```

Rules:

- Domain knows no UI, Ktor, Room, Android, or platform details.
- Data implements domain repository contracts.
- Presentation depends on domain-facing APIs, not DAOs or HTTP clients.
- Platform source sets provide only unavoidable platform implementations.

## Suggested Shared Structure

```text
shared/src/commonMain/kotlin/.../
  domain/
    model/
    repository/
    usecase/
    error/

  data/
    remote/
      api/
      dto/
    local/
      db/
      entity/
      dao/
    mapper/
    repository/

  di/
```

Platform-specific implementation should be placed in `androidMain` / `iosMain` only when required by Room, Ktor engine setup, filesystem/platform APIs, etc.

Android presentation:

```text
androidApp/src/main/.../
  presentation/
    articlelist/
    articledetail/
    navigation/
    component/
    theme/
  di/
```

Avoid feature/module proliferation for a two-screen take-home.

## Repository Contract

Keep the public contract small.

Conceptually:

```text
observeArticles() -> stream of persisted domain articles
refreshArticles() -> synchronization result
getArticle(id) -> persisted/domain article
```

Exact signatures can change, but the UI should not receive raw API DTOs or Room entities.

## Single Source of Truth

Room is the source of truth.

```text
1. UI subscribes to persisted articles.
2. Cached articles can render immediately.
3. Repository attempts remote refresh.
4. Response is validated/mapped.
5. Successful usable data is written transactionally to Room.
6. Room emits the new state.
7. UI updates from Room.
```

Do not clear valid cached content before a remote request succeeds.

## Refresh Semantics

Refreshing is separate from content availability.

Useful presentation state concept:

```text
content: List<Article>
isInitialLoading: Boolean
isRefreshing: Boolean
blockingError: DomainError?
nonBlockingMessage: UiMessage?
```

This avoids replacing valid cached content with a full-screen error during a failed refresh.

## Error Model

Translate infrastructure failures into a small domain/presentation vocabulary.

At minimum distinguish:

- network/connectivity failure,
- remote/server/API failure,
- local persistence failure,
- malformed/unusable data,
- unknown failure.

Do not leak raw exceptions to UI strings.

## Mapping Boundaries

Use explicit mappings:

```text
NewsApi DTO -> persistence entity -> domain model -> UI model (only if needed)
```

Avoid one model serving HTTP, DB, domain, and UI simultaneously.

## KMP Boundary

Aim to share:

- models,
- repository contracts,
- use cases,
- business rules,
- API DTOs and mapping,
- Ktor client/API service,
- Room entities/DAOs/database declarations where KMP supports them,
- repository implementation,
- domain errors and utilities.

Keep Android-only:

- Compose application UI,
- Android ViewModels if the chosen design keeps presentation Android-specific,
- platform context/setup,
- Android-only system APIs.

### iOS

An iOS UI is a bonus, not a core deliverable.

If low-risk, configure the shared module with an iOS target and keep platform construction behind clean boundaries. Do not jeopardize the Android deliverable merely to claim an iOS bonus.

## Performance Principles

- Stable item keys in lazy lists.
- Avoid expensive transformation in Composables.
- Expose already-prepared immutable UI state.
- Avoid unnecessary Flow re-subscription/recomposition.
- Let Coil handle image loading/caching.
- Use sensible Ktor timeout/logging configuration; avoid verbose sensitive logging in production configuration.
