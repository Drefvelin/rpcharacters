# Batch 09 — Creator stages

**Depends on:** [01](./01-config-and-loaders.md), [08](./08-prosthetic-fuel.md)  
**Blocks:** 10, 12

## Goal

Two optional character creator stages for backstory permanent injuries and one prosthetic.

## `stages.yml`

### `permanent_injury_info_stage` (type: info)

- Explain optional backstory injuries, 0 cost, many allowed
- Skippable via `auto-next` / next command like other info stages

### `permanent_injury_selection_stage` (type: selection)

```yaml
type: selection
target: trait
key: injury
filter: permanent-only   # new flag OR filter traits without duration
min-select: 0
max-select: 99           # practical cap or unlimited
# no points
gui-size: 54
slots: [...]
```

### `prosthetic_info_stage` (type: info)

- Optional, 1 point budget, one prosthetic max
- Arcane fuel disclaimer
- Skippable like evil path

### `prosthetic_selection_stage` (type: selection)

```yaml
type: selection
target: trait
key: prosthetic
min-select: 0
max-select: 1
points: 1
gui-size: 54
slots: [...]
```

## `SelectionStage` / `InventoryManager` changes

- **`filter: permanent-only`:** options = `key: injury` and trait has no `duration`
- **Icons:** distinct material/icon per injury and prosthetic tier (config or convention)
- **Skip:** allow confirm with 0 selections (like evil min-select 0)
- **Prosthetic from creator:** add trait with full fuel if arcane

## Stage graph (`stages.yml` next map)

Insert after personality or before finish, parallel to evil branch skippability.

## Web messages

Add `web-messages` blocks for both info stages (ProvinceSystem batch 10).

## Acceptance

- [x] Can skip both stages entirely
- [x] Can pick multiple permanent injuries, 0 points
- [x] Can pick at most one prosthetic, costs 1 point
- [x] Prosthetic selectable without permanent injury
- [x] All option icons distinct and clear
