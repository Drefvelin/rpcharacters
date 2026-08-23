# Batch 10 — Web catalog sync

**Depends on:** [09](./09-creator-stages.md)  
**Blocks:** 12

## Goal

ProvinceSystem character creator catalog matches in game permanent injury and prosthetic stages.

## `CreationCatalogSyncService`

| Trait | Include in catalog? |
|-------|---------------------|
| Healing injuries (`duration` set) | No |
| Permanent injuries (no `duration`, `key: injury`) | Yes |
| Prosthetics (`key: prosthetic`) | Yes |
| Runtime only | No |

## Payload fields

Per trait (existing shape + any new fields web needs):

- `id`, `name`, `key`, `cost`, `description`, `mutually_exclusive`
- `has_duration` or omit for permanent only filter on web
- `fuel_disclaimer` flag for arcane prosthetics (or infer from `fuel-template`)

## Stage sync

- Export new creator stages with same `min-select`, `max-select`, `points` as `stages.yml`
- Web applies: permanent injuries 0 cost multi select, prosthetic 1 point single select, both optional

## `RosterSyncService`

- Runtime healing injuries: sync to roster as traits (for sheet display) with remaining duration if useful
- Prosthetics: show with fuel % optional

## ProvinceSystem frontend (separate repo tasks)

- Render permanent injury multi pick
- Render prosthetic single pick with point cost
- Skip buttons on both steps
- Arcane disclaimer text

Document RPC/frontend file targets when implementing cross repo.

## Acceptance

- [x] Web catalog lists permanent injuries and prosthetics only
- [x] Point rules match in game
- [x] Created character from web has same traits and `trait-state` as in game
