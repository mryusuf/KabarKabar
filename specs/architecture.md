# Architecture Specification

## Goals

Keep the implementation small while making the offline-first behavior explicit, testable, and shared where it provides real value. Android is the required product target. iOS UI is not core scope.

## Target Module Ownership

There are no additional Gradle modules in the target architecture.

```text
androidApp
  Compose screens, ViewModels, Navigation
  durable UI state and one-shot UI effects
  Coil and application/composition wiring
          |
          v
sharedLogic
  commonMain: domain + shared data behavior
  androidMain/iosMain: unavoidable platform construction
```

`sharedUI` is an existing scaffold module, but it is not part of the target architecture. New production presentation code must not be placed there.

## Dependency Direction

```text
androidApp/presentation -----> sharedLogic/domain contracts
                                      ^
                                      |
                         sharedLogic/data implementation
```

Rules:

- Domain knows no Compose, ViewModel, Ktor, Room, Android, or platform details.
- Shared data implements domain repository contracts.
- Presentation depends on repository/use-case APIs, never on DAOs, Ktor clients, or raw data sources.
- Platform source sets contain only unavoidable engine, database-builder, filesystem, or platform API code.
- Composition wiring is owned by `androidApp`; tests may construct dependencies directly.

## Source-Set Structure

```text
sharedLogic/src/commonMain/kotlin/.../
  domain/
    model/
    repository/
    error/
    usecase/       # only meaningful boundaries
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

sharedLogic/src/androidMain/
  Android HTTP engine and Room database construction

sharedLogic/src/iosMain/
  iOS HTTP engine and Room database construction, only if the shared iOS target remains

androidApp/src/main/.../
  presentation/
    articlelist/
    articledetail/
    navigation/
    component/
    theme/
  di/
```

Ktor API/DTO/mapping behavior is shared. Ktor engine construction is platform-specific. Room entities, DAOs, and database declarations are shared where supported; database construction and filesystem paths remain platform-specific.

## Repository Contract

Keep the public contract conceptually small:

```text
observeArticles() -> Flow of typed persisted-data outcome for domain articles
observeArticle(id) -> Flow of typed persisted article, absence, or local failure
refreshArticles() -> typed success/failure synchronization result
```

The repository is the only boundary exposed to presentation. It maps infrastructure failures to domain-level errors and never exposes raw exceptions, API DTOs, or Room entities. A successful empty article observation is distinct from an observation failure; the latter is represented by the domain error vocabulary rather than an empty list or a thrown infrastructure exception.

The article ID is a stable deterministic, collision-resistant value derived from the canonical article URL. The canonical URL is stored separately. Navigation passes the stable ID, never the whole `Article`.

Use cases are optional at individual operations. Add one only when it owns a meaningful domain or presentation boundary; do not create pass-through classes for every repository method.

## Mapping Boundaries

Use explicit mappings at the data boundary:

```text
NewsAPI DTO -> validated article data -> Room entity -> domain Article
```

Do not use an API DTO or Room entity as the presentation contract. A separate UI model is optional only when it adds real value; typed presentation state remains in `androidApp`.

## Offline-First Source of Truth

Room is the exclusive readable source of article data for higher layers. Network responses never feed UI directly.

```text
1. UI observes the repository's persisted-data flow.
2. Cached data can render immediately.
3. A refresh fetches remote data first.
4. The full response is validated and mapped.
5. A successful usable snapshot replaces the cached headline snapshot transactionally.
6. Room emits the committed snapshot.
7. UI updates from persisted data.
```

Never clear valid cached content before the network request succeeds. A malformed/unusable remote response or failed database replacement preserves the previous committed cache.

## Refresh, Failure, and Empty Semantics

- Remote failure preserves existing cache.
- Malformed or unusable remote data preserves existing cache.
- A failed database replacement preserves the previous committed cache.
- A valid successful response with zero usable articles is a legitimate empty snapshot and may replace the previous snapshot.
- A valid empty snapshot produces `Empty`; failure with no usable cache produces `Error`.
- Infrastructure failures are classified into a small domain vocabulary such as network, remote/API, local persistence, malformed data, and unknown failure.
- Cancellation is not converted into a user-facing failure.

## Presentation State

Durable article-list content uses typed states:

```text
Loading
Data(articles)
Empty
Error(uiError)
```

Refreshing is orthogonal to content availability:

```text
ArticleListUiState(
  content: ContentState,
  isRefreshing: Boolean
)
```

`Data` remains visible while refreshing. A one-shot Snackbar/effect is delivered through an event mechanism such as `SharedFlow`; it is not represented as a persistent nullable `StateFlow` field. UI state contains presentation-safe messages, not raw infrastructure exceptions.

## Room

Use the current stable Room KMP API compatible with the selected Kotlin/AGP toolchain: shared database declarations with the supported `RoomDatabaseConstructor` pattern, compiler processing for every declared target, and the supported SQLite driver approach. Keep shared entities, DAOs, and database declarations in `commonMain` where supported, and check schema output into the repository.

Provide one database instance through DI. The Android builder uses the application database path; other platform builders use their platform filesystem APIs. Do not pass `Context` or platform paths into domain code. Use KMP-compatible suspend, Flow, and transaction APIs rather than assuming Android-only Room APIs.

No speculative migration framework is required. Start with an explicit schema version and add a real migration only when the schema changes.

## Ktor and Configuration

- Keep API models, serialization, mapping, request behavior, timeout configuration, and error mapping shared.
- Construct the platform HTTP engine in the relevant platform source set.
- Inject API configuration into shared code from the application composition root.
- Keep the NewsAPI key in local-only configuration.
- Never include the API key in request logs, headers logs, exception messages, or UI state.

## Performance and Simplicity

- Use stable lazy-list keys based on the stable article ID.
- Avoid expensive transformation in Composables.
- Avoid duplicate refreshes and unnecessary Flow subscriptions.
- Share one Coil `ImageLoader` at the Android application boundary.
- Do not add pagination, extra modules, a full-screen image viewer, or iOS UI before core behavior is green.
