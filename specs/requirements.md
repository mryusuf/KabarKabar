# Requirements

## 1. Product

Build a two-screen News Reader using NewsAPI.

### Accepted NewsAPI country clarification

The take-home PDF used `country=id` as a sample request. During development it
returned no current articles. Inosoft explicitly approved using `country=us`,
currently the only country returning non-empty data, for the submitted app.
The country selector and automatic fallback are optional future scope and are
not required for this phase. Empty-result behavior remains covered by
deterministic tests rather than a live country response.

### Article List

Must:

- show title,
- show short description,
- show image,
- show formatted publish date,
- support pull-to-refresh,
- show loading during initial fetch/refresh,
- show cached Room data when offline or remote fetch fails,
- preserve cached content on refresh failure,
- show a non-blocking error when refresh fails but cache exists,
- show a meaningful error when no usable cache exists,
- handle an empty result gracefully.

### Article Detail

Must:

- show full title,
- show description,
- show image,
- show formatted publication date,
- support system back,
- support app-bar back.

## 2. Architecture

Must use:

- Kotlin Multiplatform,
- Clean Architecture with Domain / Data / Presentation separation,
- ViewModel + StateFlow,
- Ktor Client with logging and timeout configuration,
- Room for KMP,
- Koin or another KMP-compatible DI approach,
- Jetpack Compose + Navigation Compose,
- Coil.

Target ownership is fixed:

- `androidApp` owns Compose screens, ViewModels, Navigation, durable UI state, one-shot UI effects, Coil, and application/composition wiring.
- `sharedLogic` is the KMP module containing shared domain and data behavior.
- `sharedLogic/commonMain` contains domain contracts/models and shared API, mapping, repository, and Room behavior where supported.
- `sharedLogic/androidMain` / `sharedLogic/iosMain` contain only required platform HTTP-engine and database construction.
- The existing `sharedUI` module is not part of the target architecture.
- iOS UI is not core scope.

Meaningful shared code is required. A nominal KMP module without shared business/data logic is not sufficient.

The readable article source for higher layers is persisted Room data. Network responses never feed UI directly.

The conceptual repository contract is:

```text
observeArticles()
observeArticle(id)
refreshArticles()
```

Each article uses a stable deterministic local ID derived from its canonical URL. The canonical URL is preserved separately, and Navigation passes the stable ID rather than the whole article.

## 3. Offline-First Acceptance Scenarios

### R1 - First launch, online

Given no cached articles,
when the app starts with a working network,
then it fetches NewsAPI,
validates/maps the full usable response,
replaces the persisted headline snapshot transactionally,
and displays the committed persisted articles.

### R2 - Subsequent launch with cache

Given articles already exist in Room,
when the screen opens,
then cached articles are displayed without waiting for network synchronization.

### R3 - Offline with cache

Given articles exist in Room,
when remote synchronization fails,
then cached articles remain visible.

### R4 - Refresh failure with cache

Given cached articles are visible,
when pull-to-refresh fails,
then the existing list remains visible
and a non-blocking error is surfaced.

### R5 - No remote and no local data

Given no usable cached articles,
when remote fetch fails,
then the user sees a meaningful recoverable error state rather than a blank screen.

### R6 - Empty successful result

Given a successful request produces no usable articles,
then the UI displays a deliberate empty state.

Failure with no usable cache is `Error`, not `Empty`. A valid successful response containing zero usable articles is a legitimate empty snapshot and may replace the previous snapshot.

Remote failure, malformed/unusable remote data, and failed database replacement must preserve the previous committed cache. Raw infrastructure exceptions must not reach presentation.

Durable presentation content uses typed `Loading`, `Data`, `Empty`, and `Error` states. Refreshing is orthogonal to content availability, and cached `Data` remains visible during refresh. One-shot Snackbar/effect messages are not persistent nullable `StateFlow` data.

## 4. AI-Assisted Development

Android Studio Agent Mode must be used for at least 3 meaningful engineering tasks.

For each logged task record:

- prompt/task,
- what the agent produced,
- what was changed or rejected,
- why,
- how the result was validated.

At least 1 real example must show a generated mistake or suboptimal implementation being corrected.

## 5. Testing

Core behavior must have meaningful unit and UI coverage.

Exact required scenarios are in `specs/testing.md`.

## 6. Security

- Never commit a real NewsAPI key.
- Use local-only configuration.
- Provide an example configuration or complete setup instructions.

## 7. Scope Interpretation

The source document lists pull-to-refresh once as expected Article List behavior and again under bonus features.

Decision: **implement pull-to-refresh as core** because it is explicitly described in the required screen behavior and is low-cost relative to the ambiguity.

Deferred until core is green:

- pagination/load more,
- full-screen image viewer,
- extra modularization,
- iOS UI application,
- nonessential polish.

Dark mode and `commonTest` coverage are good low-risk bonuses after core.
