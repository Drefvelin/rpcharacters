# Batch 07 — Prosthetics install and swap

**Depends on:** [01](./01-config-and-loaders.md), [02](./02-trait-state-persistence.md), [03](./03-trait-runtime-effects.md)  
**Blocks:** 08, 12

## Goal

Right click with a prosthetic item to install it for a mapped permanent injury, or swap an existing prosthetic in that group (no refund).

## `ProstheticInstallListener`

`PlayerInteractEvent` (RIGHT_CLICK_AIR / RIGHT_CLICK_BLOCK), same pattern as tome listeners.

### Resolve action

1. Match held item to a trait → item path in `prosthetics.yml`
2. Look at that injury group on the character
3. Determine target:
   - **Install:** has the permanent injury, no prosthetic from that group
   - **Already owned:** already has the trait that item installs
   - **Replace:** has a different prosthetic in that group (confirm GUI; old trait is destroyed, not returned as an item)

### Install

- Consume 1 of the held item
- Remove permanent injury trait
- Add the prosthetic trait for that item
- Init fuel to full capacity if fueled
- Messages + sound

### Replace

- Confirm inventory (`ProstheticConfirmHolder`, not character-menu confirm)
- On confirm: consume 1 item, remove current prosthetic, add the new one
- Fuel on the old trait is discarded; incoming fueled traits start full
- Cancel or close does not consume

## Validation

- Cannot install if no matching permanent injury and no prosthetic in that group
- Same item/trait again: "already have that prosthetic"
- Swap allowed in either direction; no refund (blocks farming creator prosthetics as items)

## `TraitChangeService` hooks

- `replaceInjuryWithProsthetic(player, character, injuryId, prostheticId)`
- `replaceProsthetic(player, character, fromId, toId)`

## Acceptance

- [x] Right click `m.utils.wood_claw_arm` with `one_handed` installs `wooden_claw_arm`
- [x] Right click a different mapped item with an existing prosthetic opens replace confirm
- [x] Permanent injury removed; injury count decreases
- [x] Creator picked prosthetic without injury: install path skipped; item can still replace the owned prosthetic after confirm

## Implemented

- `ProstheticLoader.resolveForItem(ItemStack)` returns a single trait+item match
- `TraitChangeService.replaceInjuryWithProsthetic` and `replaceProsthetic` (no fuel % migrate)
- `ProstheticInstallListener` registered in `RPCharacters`
