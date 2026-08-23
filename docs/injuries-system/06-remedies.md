# Batch 06 — Remedies

**Depends on:** [01](./01-config-and-loaders.md), [02](./02-trait-state-persistence.md)  
**Blocks:** 11, 12

## Goal

Remedies instantly cure **healing** injuries only.

## `RemedyListener` changes

In `findCurableTrait`:

- Trait must be in remedy whitelist (unchanged)
- Trait must be healing: `TraitData.hasDuration()` or `InjuryProgressionLoader.isHealingTrait(id)`
- Permanent injuries (`blind`, `one_handed`, `one_legged`, …) → skip

## `items.yml`

```yaml
remedies:
  healing_draught:
    item: v.potion
    traits:
      - broken_arm
      - broken_leg
      - half_blind
```

No permanent ids in list.

## Edge cases

- Consume with no curable healing injury: no effect, do not cancel consume (locked behavior)

## Acceptance

- [x] Remedy removes healing `broken_arm`
- [x] Remedy does nothing on `one_handed` or `blind`
- [x] Clears `trait-state` for removed trait

## Implemented

- `RemedyListener.findCurableTrait` filters by `InjuryProgressionLoader.isHealingTrait` and `current.hasDuration()`
- `RemedyLoader` validates remedy trait ids at load (after traits); skips non-healing entries with warnings
- `remedyLoader.load()` moved after trait/injury loaders in `RPCharacters.loadConfigs()`
