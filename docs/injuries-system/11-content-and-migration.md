# Batch 11 — Content and migration

**Depends on:** [04](./04-permadeath-flow.md), [06](./06-remedies.md), [07](./07-prosthetics-install.md)  
**Blocks:** 12

## Goal

Author balanced YAML content and update player facing copy.

## Injury traits (`traits/injury-traits.yml`)

| Id | Type | Notes |
|----|------|-------|
| `broken_arm` | healing + duration | full penalty -4 str / -2 con |
| `one_handed` | permanent | -3 str / -1 con; prosthetic target |
| `broken_leg` | healing | full penalty -4 dex |
| `one_legged` | permanent | -3 dex + slowness; prosthetic target |
| `half_blind` | healing | dex -1 at full scale |
| `blind` | permanent | no duration, no prosthetic |

Tune attribute modifiers: **healing full penalty > permanent > prosthetic T1 > T2 > arcane powered > healthy**.

Obsolete doc ids `permanent_broken_arm` / `permanent_broken_leg` were never implemented; use `one_handed` / `one_legged`.

## Prosthetic traits (`traits/prosthetic-traits.yml`)

| Id | Tier | Fuel |
|----|------|------|
| `wooden_claw_arm` | 1 arm | no |
| `basic_prosthetic_arm` | 2 arm | no |
| `arcane_prosthetic_arm` | 3 arm | arcane_fuel |
| `pegleg` | 1 leg | no |
| `basic_prosthetic_leg` | 2 leg | no |
| `arcane_prosthetic_leg` | 3 leg | arcane_fuel |

## Permanent injuries for creator

Creator stages use `one_handed`, `one_legged`, and `blind` (filter: permanent-only).

## `zones.yml` tutorial

- Death in zone may permakill or add a healing injury
- Healing injuries recover while active online, or become permanent on another zone death
- Remedies cure healing injuries only
- Prosthetics replace some permanent injuries
- More injuries raise permakill chance

## `PermadeathRisk` / inventory lore

- `+N% per injury` from `chancePerInjury`
- Healing → permanent on death; remedies healing only; prosthetics for some permanent injuries
- Traits GUI uses `TraitEffectResolver.resolveDisplayName` for injury/prosthetic traits

## Acceptance

- [x] All six prosthetics + injury set load
- [x] Balance review: arcane powered (-1) better than `one_handed` (-3 str)
- [x] Tutorial text and permadeath lore accurate
