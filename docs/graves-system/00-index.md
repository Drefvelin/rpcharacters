# Graves System - Batch Index

**Repos:** `Workspace/rpcharacters` (owner) and `Workspace/thievery` (steal hook)

**Design lock:** [phase0-design.md](./phase0-design.md)

**Depends on:** Active character name via `DisplayIdentityService.resolveCharacterName` (`%rpcharacters_name%`). PvP knockout already prevents many deaths; graves only spawn on real `PlayerDeathEvent`.

**Naming:** This feature is **graves**. Do not name packages, classes, configs, messages, or comments after any third-party death-chest plugin.

## Goal

On death, keep loot in a chest at the last solid ground the player stood on. Owner right-clicks to take items (overflow drops). Thieves never open a GUI; Thievery takes stealable items they can fit. Excluded inventory slots stay on the corpse inventory / never enter the grave.

## Locked rules (summary)

| Piece | Choice |
|-------|--------|
| Name | Graves (`grave` package, `graves.yml`) |
| Death listener | `PlayerDeathEvent` at `HIGHEST` (mutate drops last) |
| Spawn location | Last solid snapshot (1s tick), not void/air death pos |
| Block | Vanilla chest + PDC + Gson persist |
| Hologram | Per-viewer fake `TextDisplay` (ProtocolLib). Line 1: character name. Optional killer line. Rob hint only for eligible killers |
| Owner click | No GUI. Give all items into inventory. Overflow drops at grave. Then remove grave if empty |
| Thief click | No GUI. Thievery greedy steal: budget + space. Remainder stays in grave |
| Lock | Protected graves: only killer UUID may steal. Owner always recovers |
| Excluded slots | Config Bukkit player-inventory indices. Default top inventory row, left 4: `9, 10, 11, 12` |
| XP | Stored on grave. Owner receives on recover. Thieves do not take XP |
| RPC / Thievery | RPC never imports Thievery. Thievery depends on RPCharacters |

## Batches (implement in order)

| # | Doc | Deliverable |
|---|-----|-------------|
| 1 | [01-config-and-tracker](./01-config-and-tracker.md) | `graves.yml`, loader, last-solid tracker |
| 2 | [02-grave-data](./02-grave-data.md) | `Grave` model, persist, PDC, lookup |
| 3 | [03-death-and-hologram](./03-death-and-hologram.md) | HIGHEST death, spawn chest, hologram |
| 4 | [04-interact](./04-interact.md) | Owner recover, protect, hopper/break |
| 5 | [05-thievery-hook](./05-thievery-hook.md) | Remove old death-chest hook, steal from graves |
| 6 | [06-verify-and-deploy](./06-verify-and-deploy.md) | Test matrix, disable other death-chest plugins |

## Checkpoint

```text
walk solid ground → snapshot every second
real death (HIGHEST) → skip excluded slots → chest at snapshot
owner click → items into inv, rest drop, grave gone
non-owner + locked + not killer → locked
killer or unlocked → Thievery steal (budget + space), rest stays
```

**Done when:** Graves spawn without a third-party death-chest plugin; Thievery steals from graves; excluded slots never appear in the chest.

## Status

Batches 1-5 implemented. Batch 6: RPCharacters then Thievery packaged and copied into the live `plugins` folder; the competing death-chest jar is not loadable. In-game checklist remains [06-verify-and-deploy.md](./06-verify-and-deploy.md).
