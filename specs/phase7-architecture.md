# Phase 7 Architecture Extensions

This document describes only bonus-related architectural changes. Phase 0–6 remains the base contract.

## 1. Baseline

```text
Android Compose
      |
Android ViewModels
      |
shared domain repository contract
      |
OfflineFirstArticleRepository
   /               \
Ktor              Room
remote              |
                exclusive read source
```

`release-v1.0.0` is the immutable fallback.

## 2. Phase 7A / 7B

7A and 7B are presentation-only unless a real defect requires otherwise.

They must not modify repository semantics, Room schema, network request behavior, Article identity, or T1–T5 contracts.

Reusable Android UI components are acceptable inside `androidApp`; do not introduce a design-system module.

## 3. Phase 7C — Country as explicit shared scope

### 3.1 Country model

Prefer a small shared model, conceptually:

```text
NewsCountry
- US(code = "us")
- ID(code = "id")
```

### 3.2 No hidden mutable repository country

Avoid:

```text
repository.currentCountry = ...
repository.observeArticles()
```

Prefer:

```text
observeArticles(country)
refreshArticles(country)
observeArticle(country, id)
```

### 3.3 Persistence

Country ownership must be represented in persistence.

A straightforward option:

```text
ArticleEntity(
  countryCode,
  articleId,
  ...
)
PRIMARY KEY(countryCode, articleId)
```

DAO operations become country-scoped:

```text
observeArticles(country)
observeArticle(country, id)
replaceSnapshot(country, articles)
appendPage(country, articles) // 7D
```

Exact schema may differ if an equivalent snapshot/membership model is cleaner.

Critical invariant:

```text
mutation(country = X) must not mutate rows owned by country = Y
```

### 3.4 Migration

The pre-7C database represents the current US submission cache.

Migration should preserve existing rows as US-scoped data.

Destructive migration is not acceptable for this bonus.

## 4. Phase 7D — Paging extension

### 4.1 Remote boundary

Conceptually:

```text
fetchTopHeadlines(country, page, pageSize)
```

Response metadata should expose enough information to determine whether another page can be requested.

### 4.2 Repository operations

Keep refresh and paging explicit:

```text
observeArticles(country)
refreshArticles(country)
loadMoreArticles(country)
observeArticle(country, id)
```

### 4.3 Persistence atomicity

Refresh:

```text
validate page 1 completely
-> transactionally replace selected-country snapshot
```

Load more:

```text
validate next page completely
-> transactionally append/dedupe selected-country rows
```

Failed load-more leaves previously committed rows unchanged.

### 4.4 Paging metadata

Keep paging metadata minimal. Potential state:

```text
country
nextPage
hasMore
```

Do not store UI-only loading flags in Room.

## 5. Phase 7E — iOS presentation boundary

### 5.1 Shared logic remains source

Swift consumes shared data/domain behavior. Do not duplicate NewsAPI, persistence, refresh rules, country isolation, or paging dedupe in Swift.

### 5.2 Native presentation adapter

Conceptually:

```text
SwiftUI View
   |
@MainActor observable adapter
   |
shared repository / shared façade
```

The adapter maps shared outcomes into iOS-native presentation state.

### 5.3 Flow bridging

Use the simplest toolchain-compatible approach for observing Kotlin flows from Swift.

Requirements:

- explicit lifecycle/cancellation,
- UI-safe state delivery,
- observations cancelled when owner is released,
- no leaked long-lived collectors.

Avoid adding a broad reactive bridge library unless the current toolchain clearly benefits from it.

### 5.4 Framework export

Export only shared APIs Swift actually needs. Avoid unnecessary transitive export. Prefer local monorepo integration.

## 6. Dependency rule

```text
Android presentation ─┐
                     ├─> shared domain/data
iOS presentation ─────┘
```

Shared code never depends on either platform UI.
