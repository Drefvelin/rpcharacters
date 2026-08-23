# Batch 02 — Trait state persistence

**Depends on:** [01-config-and-loaders](./01-config-and-loaders.md)  
**Blocks:** 03, 05, 07, 08

## Goal

Persist per trait instance state on characters and migrate missing fields on load.

## Trait ids (locked)

| Id | Type |
|----|------|
| `one_handed` | permanent arm loss (legacy id) |
| `one_legged` | permanent leg loss (legacy id) |
| `broken_arm` | healing arm injury |
| `broken_leg` | healing leg injury |
| `half_blind` | healing |
| `blind` | permanent |

Progression: `broken_arm` → `one_handed`, `broken_leg` → `one_legged`, `half_blind` → `blind`.

No JSON trait id rewriting. Saved `one_handed` stays `one_handed`.

## Data model

`RPCharacter` field:

```java
Map<String, TraitInstanceState> traitState; // keyed by trait id (lowercase)
```

`TraitInstanceState`:

| Field | Type | Used by |
|-------|------|---------|
| `durationRemainingMs` | long | Healing injuries |
| `fuel` | double | Fueled prosthetics |

## Database (`Database.java`)

**Save** character JSON:

```json
"trait-state": {
  "broken_arm": { "duration-remaining-ms": 172800000 },
  "arcane_prosthetic_arm": { "fuel": 42.5 }
}
```

**Load:**
1. Parse `trait-state` or default `{}`
2. For each trait on character, apply defaults (see below)
3. No trait id migration in Database

## Defaults on load

| Trait type | Missing state |
|------------|---------------|
| Healing (`duration` in YAML) | `duration-remaining-ms` = full duration from trait def |
| Fueled prosthetic | `fuel` = `fuel-capacity` from trait def |
| Other | no state entry required |

## API on `RPCharacter`

- `getTraitState(traitId)`, `setDurationRemaining`, `setFuel`, `removeTraitState`
- `initializeTraitState` on `addTrait`, clear on `removeTrait`
- `ensureTraitStateDefaults()` after load

## Acceptance

- [x] Old characters without `trait-state` load cleanly
- [x] Round trip save/load preserves duration and fuel
- [x] Legacy ids (`one_handed`, `one_legged`) resolve without JSON rewrite

## Status

**Done** (batch 02 implemented).
