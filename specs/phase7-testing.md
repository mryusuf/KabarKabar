# Phase 7 Testing & Acceptance Plan

Phase 7 tests are regression protection, not replacements for T1–T5.

## Global regression gate

After each Android/shared subphase:

```bash
./gradlew clean \
  :sharedLogic:allTests \
  :sharedLogic:assemble \
  :sharedLogic:check \
  :androidApp:testDebugUnitTest \
  :androidApp:assembleDebug \
  :androidApp:lintDebug

./gradlew :androidApp:connectedDebugAndroidTest

git diff --check
```

When iOS UI exists, add its build/test gate separately.

# 7A Acceptance

## A1 — System theme

- light mode List/Detail coherent,
- dark mode List/Detail coherent,
- Loading/Empty/Error readable.

## A2 — Prominent first item

Given at least two articles:

- first receives prominent layout,
- second+ use compact rows,
- no "Featured" semantics/label,
- tapping either navigates correctly.

## A3 — Optional content

- first article without image safe,
- row without image safe,
- missing description safe.

## A4 — Accessibility baseline

- useful semantics,
- decorative/absent images not misleading,
- larger font scale leaves core actions usable.

# 7B Acceptance

## B1 — Viewer open/close

```text
tap hero -> viewer -> close/back -> same Detail
```

## B2 — No image

No misleading viewer action and no crash.

## B3 — Viewer offline

Cached image may render offline; offline image availability is not guaranteed.

# 7C Acceptance

## C1 — US and ID isolation

```text
seed US A
seed ID B
observe US -> A
observe ID -> B
```

## C2 — US refresh cannot mutate ID

```text
ID B
refresh US -> US C
ID remains B
```

## C3 — ID empty cannot clear US

```text
US A
ID [] -> ID Empty
US still A
```

## C4 — ID failure cannot clear US

US remains unchanged.

## C5 — switch with cache

Switching to cached country shows its persisted data without waiting for network.

## C6 — switch without cache

```text
pending -> Loading
success [] -> Empty
failure -> Error
```

## C7 — migration

Pre-7C cached rows migrate as US without destructive reset.

# 7D Acceptance

## D1 — append

```text
page1 A,B
page2 C,D
-> A,B,C,D
```

## D2 — dedupe

```text
page1 A,B
page2 B,C
-> A,B,C
```

## D3 — load-more failure

```text
A,B visible
page load fails
-> A,B remain
-> progress stops
-> Load more button available
```

## D4 — refresh after pagination

Refresh page 1 resets the paged snapshot coherently; stale later-page rows must not be mixed accidentally into the fresh result.

## D5 — country isolation

US paging never appends into ID and vice versa.

## D6 — duplicate triggers/concurrency

Repeated load-more triggers cannot duplicate pages, stale-overwrite, or duplicate rows.

# 7E Acceptance

## E1 — iOS launch

- framework integrates,
- SwiftUI app launches in iOS Simulator,
- no runtime Room/Ktor/framework construction failure.

## E2 — list/detail

- List renders,
- tap -> native Detail,
- back -> List.

## E3 — refresh/offline

- refresh uses shared synchronization,
- cached content remains visible on failure,
- no-cache failure gives blocking error.

## E4 — country

If 7C retained, iOS US/ID uses shared country-scoped behavior.

## E5 — pagination

If 7D retained, load-more uses shared paging behavior and preserves list on failure.

## E6 — native presentation

- system dark/light,
- Dynamic Type/basic accessibility,
- no direct NewsAPI/SQL from SwiftUI.

## E7 — lifecycle

- observations cancelled when owner is released,
- repeated navigation does not create duplicate long-lived collectors.

# Manual QA principle

Use emulator/simulator/Computer Use for visual evidence, but do not replace deterministic logic tests with screenshots alone.
