# Injuries System — Batch Index

**Repo:** `Workspace/rpcharacters`  
**Design lock:** [phase0-design.md](./phase0-design.md)  
**Depends on:** Existing permadeath zones, trait loader, character creator, remedies, ProvinceSystem web catalog sync

## Goal

Healing vs permanent injuries, progression on zone death, remedies, prosthetic replacements with optional fuel, and creator/web backstory stages.

## Locked rules (summary)

| Piece | Choice |
|-------|--------|
| Injury traits | Specific ids (`broken_arm`, `broken_leg`, `half_blind`, `blind`, …) |
| Healing | `duration` on trait YAML; `trait-state.duration-remaining-ms`; tick online + active only |
| Zone death | Roll permakill → convert all healing OR roll new pool injury (convert replaces roll) |
| Progression | `injury-progression.yml` map; not hardcoded |
| Remedies | Instant full cure on healing injuries only |
| Prosthetics | `prosthetics.yml` injury → tier list; install/upgrade via right click |
| Fuel | `fuel-templates.yml`; arcane prosthetics; powered/depowered same trait id |
| Permadeath count | `key: injury` only; prosthetics excluded |
| Creator | Two skippable stages: permanent injuries (0 pts), one prosthetic (1 pt) |
| Blindness | No prosthetic in config = no fix; `half_blind` heals; maps to `blind` on zone death |

## Batches (implement in order)

| # | Doc | Deliverable |
|---|-----|-------------|
| 1 | [01-config-and-loaders](./01-config-and-loaders.md) | YAML schema, loaders, default resources |
| 2 | [02-trait-state-persistence](./02-trait-state-persistence.md) | `trait-state` save/load, migration, legacy id map |
| 3 | [03-trait-runtime-effects](./03-trait-runtime-effects.md) | Duration scaling, powered/depowered trait blocks |
| 4 | [04-permadeath-flow](./04-permadeath-flow.md) | Death order, progression convert, pool roll, risk count |
| 5 | [05-healing-tick](./05-healing-tick.md) | Online active tick, heal completion |
| 6 | [06-remedies](./06-remedies.md) | Healing only instant cure |
| 7 | [07-prosthetics-install](./07-prosthetics-install.md) | Install, upgrade, remove injury trait |
| 8 | [08-prosthetic-fuel](./08-prosthetic-fuel.md) | Burn tick, refuel, depowered swap |
| 9 | [09-creator-stages](./09-creator-stages.md) | Two stages, skip, icons, disclaimers |
| 10 | [10-web-catalog-sync](./10-web-catalog-sync.md) | ProvinceSystem catalog rules |
| 11 | [11-content-and-migration](./11-content-and-migration.md) | Trait YAML content, remedy list, tutorial text |
| 12 | [12-verify-and-deploy](./12-verify-and-deploy.md) | Test matrix, deploy runbook |

## Checkpoint

```text
zone death → permakill roll → convert OR new healing injury
healing ticks down while active → remedy cures OR progresses to permanent on death
permanent injury → right click install prosthetic → upgrade tiers → arcane fuel/refuel
creator + web: optional permanent injuries + one prosthetic
```

**Done when:** All batches pass verify matrix; live configs migrated; web creator matches in game.

## Status

**01–11 done.** Batch 12 not started.
