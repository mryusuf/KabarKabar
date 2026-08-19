# Phase 7 Release & Fallback Policy

## Immutable baseline

`release-v1.0.0` is the known-good Phase 0–6 release.

Do not retag, force-update, rewrite, or delete it.

## Recommended checkpoints

Optional checkpoint tags:

```text
phase7a-polish
phase7b-image-viewer
phase7c-country
phase7d-pagination
phase7e-ios
```

These are convenience checkpoints, not release tags.

## Final release

Only create `release-v1.1.0` after:

1. all retained bonuses are reviewed,
2. full Android tests pass,
3. iOS build/tests pass if 7E is retained,
4. clean-clone setup passes,
5. public remote matches candidate,
6. secret/history scan remains clean,
7. README bonus/known-issues sections are truthful.

## Drop policy

If a later bonus destabilizes the app, do not weaken tests or core semantics just to retain it. Drop the bonus or return to the last green checkpoint.
