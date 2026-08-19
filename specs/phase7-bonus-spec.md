# Phase 7 Bonus Specification

## 1. Purpose

Phase 7 adds evaluator-visible polish and carefully selected bonus functionality on top of the audited `release-v1.0.0` baseline.

The goal is **not** to maximize feature count. The goal is to make the existing production-minded KMP architecture visible within the first minute of using the application while preserving the reliability established in Phases 0–6.

## 2. Global invariants

All Phase 7 work must preserve:

- Room as the exclusive readable source of article content.
- Network responses never feed UI article content directly.
- Existing cache remains visible during refresh.
- Failed refresh never erases valid cache.
- Successful empty data remains distinct from failure.
- Stable URL-derived `ArticleId`.
- Navigation passes stable IDs, not serialized article objects.
- Android presentation remains in `androidApp`.
- Shared domain/data logic remains in `sharedLogic`.
- T1–T5 remain green.
- No real API key is committed.
- `release-v1.0.0` remains an immutable fallback.

Every Phase 7 bonus is independently optional. A bonus that weakens submission reliability must be dropped rather than forcing the architecture around it.

---

# 3. Phase 7A — Dark Mode + Visual Polish + Baseline Accessibility

## 3.1 Theme

Support system-driven light/dark appearance.

Requirements:

- follow system theme automatically,
- no settings screen or manual theme selector in 7A,
- both modes retain readable contrast,
- system/status/navigation surfaces visually fit the app,
- do not hardcode colors throughout feature Composables.

## 3.2 Article List visual hierarchy

The first article in a non-empty list receives a prominent visual treatment.

Important:

- it is **not** semantically "Featured",
- do not add a "Featured" label,
- do not imply editorial ranking beyond list order,
- clicking it behaves exactly like any other article.

Expected direction:

```text
KabarKabar

Latest Headlines

┌──────────────────────────────────┐
│        prominent first image     │
│                                  │
│ First headline                   │
│ Short description...             │
│ formatted publication date       │
└──────────────────────────────────┘

[thumbnail] Second headline
            description...
            date

[thumbnail] Third headline
            description...
            date
```

Requirements:

- first item uses a larger image and stronger typography,
- subsequent items use compact, smooth-scrolling news rows,
- title remains the strongest textual element,
- descriptions remain subordinate,
- dates are visually consistent,
- missing image/description degrade gracefully,
- stable LazyColumn keys remain based on article identity,
- no new business semantics are introduced.

## 3.3 Article Detail polish

Expected hierarchy:

```text
< Back

[ large hero image ]

formatted date

Full article title

Description / summary
```

Requirements:

- hero image remains prominent,
- title and copy have comfortable hierarchy and spacing,
- optional image/description remain safe,
- no full article body is required,
- no detail pull-to-refresh is required.

## 3.4 Loading / Empty / Error polish

All durable states should look intentional.

### Loading

- clear visual loading state,
- no fake article skeleton data,
- avoid distracting indefinite animation.

### Empty

- clearly communicates no headlines are currently available,
- should not resemble a failure,
- retry may be offered if consistent with existing ViewModel semantics.

### Blocking Error

- clear presentation-safe message,
- clear retry affordance,
- no raw exception strings.

### Refresh failure with cache

- cached list remains visible,
- non-blocking message only,
- no full-screen error.

## 3.5 Accessibility baseline

At minimum:

- meaningful controls have content descriptions where needed,
- decorative images do not produce useless announcements,
- tappable targets remain comfortably sized,
- headings/read order remain sensible,
- color is not the only error/state signal,
- larger font scales do not obviously destroy layouts,
- light/dark contrast remains reasonable.

Do not build a custom accessibility abstraction layer.

---

# 4. Phase 7B — Full-Screen Image Viewer

The Article Detail hero image may open a full-screen viewer.

Requirements:

- only available when a usable image URL exists,
- tap hero image -> full-screen presentation,
- system back closes viewer,
- visible close/back affordance closes viewer,
- image failure remains graceful,
- no article/network fetch is triggered solely by opening viewer,
- no new image-loading architecture.

Optional: pinch/zoom only if low-risk. Zoom is not an acceptance requirement.

---

# 5. Phase 7C — Country Selection + Country-Aware Offline Correctness

## 5.1 Supported countries

Exactly two user-facing options:

- `🇺🇸 US`
- `🇮🇩 ID`

```text
🇺🇸 US -> us
🇮🇩 ID -> id
```

## 5.2 Selection UX

- current country clearly visible,
- emoji + country label,
- accessible selector,
- switching country stays on Article List,
- no settings screen.

Persisting selection across launches is optional. If not persisted, default to US and document it.

## 5.3 Country-aware cache invariant

```text
US cache != ID cache
```

Switching to ID must never delete US cache. Switching to US must never delete ID cache.

A valid empty ID response may produce ID Empty but must not clear US data. ID failure must not affect US data, and vice versa.

## 5.4 Persistence

Use the smallest robust country-owned schema. A composite `(countryCode, articleId)` primary identity or equivalent membership/snapshot design is acceptable.

Migration from the existing schema must preserve current cached rows as US data unless evidence shows another safer interpretation.

## 5.5 Repository behavior

Prefer explicit scope conceptually:

```text
observeArticles(country)
refreshArticles(country)
observeArticle(country, articleId)
```

Avoid hidden mutable repository country state.

## 5.6 State behavior on switch

### Cache exists
show selected-country cache immediately, then synchronize.

### No cache, synchronization pending
Loading.

### Successful empty result
Empty.

### Failure with no cache
blocking Error.

### Failure with cache
cached Data remains + non-blocking message.

---

# 6. Phase 7D — Pagination / Load More

NewsAPI Top Headlines supports `page` and `pageSize`.

## 6.1 UX

- load more near list end or via footer trigger,
- keep existing rows visible,
- small footer progress while loading.

If load-more fails:

```text
existing list remains
+
[ Load more ]
```

No blocking Error is shown for page-N failure.

## 6.2 Refresh vs load-more

Refresh:

```text
page 1 -> validate -> transactionally replace/reset selected-country snapshot
```

Load more:

```text
next page -> validate -> append/dedupe selected-country persisted data
```

## 6.3 Paging state

Be able to answer:

- current country,
- loaded extent/page,
- whether more results are available,
- load-more in progress,
- load-more retryability.

Use response metadata such as total result count where appropriate. Do not infer all paging correctness from list size alone.

## 6.4 Deduplication

Use existing stable ArticleId. Duplicate articles across pages appear once.

## 6.5 Country interaction

Pagination is country-scoped. No US paging operation may mutate ID state, and vice versa.

---

# 7. Phase 7E — Native iOS UI

## 7.1 Goal

Demonstrate that the shared KMP layer is genuinely reusable by adding native SwiftUI UI.

Android remains the required deliverable and must remain green.

## 7.2 Architecture

Use native SwiftUI. Do not move Android presentation into `commonMain` merely to increase shared-code percentage.

```text
sharedLogic
  - domain
  - repository/data
  - Ktor
  - Room
  - country behavior
  - pagination behavior
          |
          +-------------------+
          |                   |
     Android Compose       iOS SwiftUI
```

Use a thin native observable/presentation adapter. No networking or SQL is reimplemented in Swift.

## 7.3 Minimum iOS flow

- app launches,
- Article List renders,
- online sync populates shared persistence,
- cached content can render,
- pull-to-refresh,
- tap -> Detail,
- native back,
- offline cached list/detail,
- graceful Empty/Error.

If retained, reuse 7B viewer, 7C country selector, and 7D paging behavior through shared logic.

## 7.4 Native conventions

Prefer native SwiftUI navigation, `.refreshable` where appropriate, system light/dark, Dynamic Type, and native accessibility semantics.

Do not pixel-match Android Material UI.

## 7.5 KMP integration

Use the simplest local integration compatible with the repository/toolchain. Export only what Swift needs; avoid broad/transitive exports.

---

# 8. Explicit non-goals

Phase 7 does not include:

- architecture modularization experiments,
- extra Android feature modules,
- settings screen,
- manual theme preference,
- full NewsAPI article-body retrieval,
- detail pull-to-refresh,
- more than US and ID,
- speculative caching frameworks,
- rewriting the core app around a new architecture.

Modularization learning is deferred to Phase 8 after submission.
