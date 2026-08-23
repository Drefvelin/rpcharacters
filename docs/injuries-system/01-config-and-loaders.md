# Batch 01 — Config and loaders

**Depends on:** [phase0-design.md](./phase0-design.md)  
**Blocks:** All other batches

## Goal

Define YAML schemas and load them at plugin startup.

## New files (resources)

### `fuel-templates.yml`

```yaml
arcane_fuel:
  item: m.miscellanea.arcane_fuel
  amount-per-item: 50
  burn-rate: 1
  burn-interval: 1h
```

### `injury-progression.yml`

```yaml
progression:
  broken_arm: one_handed
  broken_leg: one_legged
  half_blind: blind
```

### `prosthetics.yml`

```yaml
replacements:
  one_handed:
    install-item: v.blaze_rod
    tiers:
      - wooden_claw_arm
      - basic_prosthetic_arm
      - arcane_prosthetic_arm
  one_legged:
    install-item: v.blaze_rod
    tiers:
      - pegleg
      - basic_prosthetic_leg
      - arcane_prosthetic_leg
```

### `traits/prosthetic-traits.yml`

Six prosthetic traits with `key: prosthetic`, tiered modifiers, arcane entries with `fuel-template`, `fuel-capacity`, `powered` / `depowered` sections.

### Update `injuries.yml`

Pool lists **healing** trait ids only (`broken_arm`, `broken_leg`, `half_blind`).

### Update `traits/injury-traits.yml`

Replace legacy ids with healing + permanent pairs. Healing traits include `duration`.

## Trait YAML extensions (`TraitLoader` / `TraitData`)

| Field | Type | Notes |
|-------|------|-------|
| `duration` | duration string | Healing only; parsed via existing duration util or new parser |
| `fuel-template` | string | Prosthetic only |
| `fuel-capacity` | double | Prosthetic only |
| `powered` | section | name override, description, attribute-modifiers, potion-effects |
| `depowered` | section | same shape |

## New loaders

| Loader | Registry API |
|--------|----------------|
| `FuelTemplateLoader` | `get(id)`, `getByItem(path)` |
| `InjuryProgressionLoader` | `getPermanent(healingId)`, `isHealingTrait(id)` via trait duration |
| `ProstheticLoader` | `getReplacement(injuryId)`, `getTierIndex(prostheticId)`, `resolveInstall(item)` |

## Bootstrap (`RPCharacters.java`)

- `createConfigs()` / `createFolders()`: add new yml files
- `loadConfigs()`: load new loaders after traits

## Acceptance

- [x] `/rpcharacter reload` loads all files without warnings
- [x] Invalid progression target logs warning and skips entry
- [x] Prosthetic tier order preserved from YAML list order

## Status

**Done** (batch 01 implemented).
