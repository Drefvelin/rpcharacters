# Batch 04 — Permadeath flow

**Depends on:** [01](./01-config-and-loaders.md), [02](./02-trait-state-persistence.md), [03](./03-trait-runtime-effects.md)  
**Blocks:** 11

## Goal

Rework `PermadeathService.handleDeath` to match locked death order.

## Flow (`PermadeathService`)

```
handleDeath(player, location):
  if not permadeath zone → return
  risk = computeRisk(character)           // injury count × chance-per-injury
  if rollPermadeath(risk) → permakill
  healing = listHealingInjuryTraits(character)
  if healing not empty:
    for each trait in healing:
      permanentId = InjuryProgressionLoader.getPermanent(trait.id)
      convertTrait(player, character, trait, permanentId)
      // remove healing trait + state, add permanent trait
    return   // conversion covers this death's injury roll
  picked = InjuryPoolLoader.pickRandom(ownedIds)
  if picked == null → log warning, return
  addTrait with full duration state initialized
  gained message + injury title
```

## `computeRisk` changes

- Count traits where `traitData.key == "injury"`
- **Remove** pool exhaustion → 100% shortcut (or replace with doc in batch 11 if still wanted for "all permanent injuries owned")
- Prosthetics never counted

## `InjuryPoolLoader`

- Pool contains healing trait ids only
- `pickRandom` excludes owned trait ids (healing + permanent + any injury key)

## `convertTrait` helper

- `TraitChangeService.removeTrait(healing)` + clear state
- `TraitChangeService.addTrait(permanent)` (no duration state)
- Messages: use permanent trait gained message

## Multiple healing injuries

Convert **all** on one death. Still no pool roll.

## Admin `/rpcharacter injure`

- Apply random **healing** injury from pool with duration state initialized
- Optional flag for permanent (admin only)

## `PermadeathRisk` lore

- Update copy if needed (no em dashes)
- Still show injury count and total %

## Acceptance

- [x] Death with healing injury converts, does not add second injury
- [x] Death with no healing injuries adds pool injury
- [x] Permakill roll happens before convert/roll
- [x] Injury count unchanged across convert

## Implemented

- `PermadeathService.handleDeath` rolls permakill first, converts all healing injuries via `InjuryProgressionLoader`, then pool pick
- `convertTrait`, `listHealingInjuryTraits`, `ownsTraitId` helpers
- Pool-exhaustion 100% risk shortcut removed from `computeRisk`
- `PermadeathRisk` lore shows injury count and permakill chance
- `/rpcharacter injure <player> [character] [permanent]` with `applyRandomPermanentInjury`
