# Batch 03 — Trait runtime effects

**Depends on:** [01](./01-config-and-loaders.md), [02](./02-trait-state-persistence.md)  
**Blocks:** 04, 05, 08

## Goal

Apply scaled healing penalties and powered/depowered prosthetic effects in `RPCharacter.update()` and integrator path.

## Healing attribute scaling

When merging trait `AttributeData` for a healing injury:

```
progress = 1 - (durationRemainingMs / totalDurationMs)
effectiveAmount = round(fullAmount * (1 - progress))  // toward 0
```

- `totalDurationMs` from trait YAML `duration`
- Clamp: never flip sign (penalty stays <= 0)
- Potion effects: use **full** YAML values always (no scaling)

## Prosthetic powered vs depowered

`Trait` or helper resolves active block:

```
if fueled && fuel > 0 → powered section
else if has depowered section → depowered section
else → base trait fields (non fueled prosthetics)
```

Depowered uses same trait id; display name/description/modifiers/potion from `depowered` block.

## `TraitChangeService` / `Integrator`

- After fuel or duration change, call `character.update()` + reintegrate if active
- `PlayerManager.traitPotionPulse()` uses resolved potion list per trait

## `InventoryManager` / trait lore

- Healing injuries: show time remaining in trait list (optional, muted)
- Fueled prosthetics: show fuel current/capacity
- Depowered: use depowered name in displays

## Acceptance

- [x] Half healed broken_arm applies ~50% attribute penalty (ints)
- [x] Slowness/weakness/blindness unchanged until trait removed
- [x] Arcane prosthetic at 0 fuel shows depowered name and modifiers

## Implemented

- `TraitEffectResolver` — scaled healing attributes, powered/depowered variant resolution, full-strength potion list
- `TraitStateFormat` — human readable duration and fuel display for trait GUI
- `RPCharacter.update()` merges `TraitEffectResolver.resolveAttributeData()`
- `PlayerManager.traitPotionPulse()` uses `resolvePotionEffects()`
- `InventoryManager.getTraitInfoItem()` and creation summary trait names use resolved display/effects + duration/fuel lore
