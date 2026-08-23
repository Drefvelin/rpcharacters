# Batch 07 — Prosthetics install and upgrade

**Depends on:** [01](./01-config-and-loaders.md), [02](./02-trait-state-persistence.md), [03](./03-trait-runtime-effects.md)  
**Blocks:** 08, 12

## Goal

Right click with install item to apply or upgrade a prosthetic for a mapped permanent injury.

## `ProstheticInstallListener`

`PlayerInteractEvent` (RIGHT_CLICK_AIR / RIGHT_CLICK_BLOCK), same pattern as tome listeners.

### Resolve action

1. Match held item to `prosthetics.yml` `install-item` (any replacement entry)
2. Find character's permanent injury traits that have a replacement entry
3. Determine target:
   - **Install:** has permanent injury, no prosthetic from that entry's tier list
   - **Upgrade:** has prosthetic at tier N, same entry, tier N+1 exists
4. If ambiguous (multiple mapped injuries on one item): prefer injury player is "fixing" or first match (document rule: one install item shared → try each owned permanent injury in config order)

### Install

- Remove permanent injury trait
- Add tier 1 prosthetic trait
- Init fuel to full capacity if fueled
- Messages + sound

### Upgrade

- Remove current prosthetic trait
- Add next tier trait
- Fuel: `newFuel = (oldFuel / oldCapacity) * newCapacity`
- Messages + sound

## Validation

- Cannot install if no matching permanent injury (unless creator backstory prosthetic only: already has prosthetic, no injury → upgrade only)
- Cannot downgrade
- Max tier: message "already best tier"

## `TraitChangeService` hooks

- `replaceInjuryWithProsthetic(player, character, injuryId, prostheticId)`
- `upgradeProsthetic(player, character, fromId, toId)`

## Acceptance

- [x] Right click `v.blaze_rod` with `one_handed` installs `wooden_claw_arm`
- [x] Second click upgrades to `basic_prosthetic_arm`, then `arcane_prosthetic_arm`
- [x] Permanent injury removed; injury count decreases
- [x] Creator picked prosthetic without injury: install path skipped until they gain injury, or tier 1 already on character from creator

## Implemented

- `ProstheticLoader.loadOrder` and `resolveForItem(ItemStack)` for config-order matching
- `TraitChangeService.replaceInjuryWithProsthetic` and `upgradeProsthetic` (fuel % preserved on upgrade)
- `ProstheticInstallListener` registered in `RPCharacters`
