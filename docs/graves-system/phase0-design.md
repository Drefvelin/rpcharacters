# Graves - Design Lock

This document is the source of truth. Implementation follows it. If code and this file disagree, change the code.

**Do not** use third-party death-chest product names in code, YAML keys, chat, holograms, or comments.

---

## Why RPC owns graves

Death chests must know character names and killer character names. RPCharacters already owns `%rpcharacters_name%` via `DisplayIdentityService.resolveCharacterName`. Thievery only steals. RPC never imports Thievery.

## Scope (basics only)

In:

- Last solid location snapshot
- Chest on death with contents (minus excluded slots)
- Text display: character name, optional killer
- Owner click recover (no GUI)
- Protection + killer UUID
- Persist across restart
- Thievery steal hook (no GUI)

Out:

- Fetch / teleport / list commands
- Graveyards, groups, paid open
- Item blacklist by lore (excluded slots cover reserved inventory)
- Fast-loot dump of the whole grave for non-owners

---

## Inventory slots (Bukkit player inventory)

Vanilla layout:

```
Main inventory top row:    9  10 11 12 13 14 15 16 17
Main inventory middle row: 18 19 20 21 22 23 24 25 26
Main inventory bottom row: 27 28 29 30 31 32 33 34 35
Hotbar (bottom bar):         0   1  2  3  4  5  6  7  8
Armor:                      36-39 (boots, legs, chest, helmet in PlayerInventory armor array)
Offhand:                    40
```

**Excluded slots (locked default):** the **top-left 4** of the **top bar of the inventory UI** = Bukkit slots **`9, 10, 11, 12`**.

Those indices are used for other systems and **must not** go into the grave.

Copy from `PlayerInventory` by slot. Excluded slots are omitted from the grave. After `getDrops().clear()`, stash excluded stacks in memory and **give them back on `PlayerRespawnEvent`** so they are not deleted. Do not drop them at the grave.

Config:

```yaml
# Bukkit player inventory slot indices that never enter a grave.
# Default: top row of the inventory, leftmost four slots.
excluded-slots:
  - 9
  - 10
  - 11
  - 12
```

If the reserved UI is actually hotbar 1-4, set `0-3` instead. Do not hardcode; always read the list.

Armor and offhand are stored unless their slot ids are listed (armor is not 9-12).

---

## Last solid location

Every **20 ticks**, if the player is online and not in spectator:

- Block at feet, or the block immediately below if standing on the top face of a solid block
- Solid = `block.getType().isSolid()` (not air, not water/lava, not void)
- Store world + block X/Y/Z (chest sits on that block, or in the air cell above if the snapshot is the ground)

On death, spawn at snapshot. If none (just joined): search down from death location for solid ground, clamp to world min Y. If still none, skip grave and let vanilla drops happen.

Do not spawn inside another grave. If the target block is occupied, search nearby (small radius, then drop vanilla).

## Death listener

`PlayerDeathEvent`, `EventPriority.HIGHEST`, `ignoreCancelled = true`.

Bukkit order: LOWEST → LOW → NORMAL → HIGH → **HIGHEST** → MONITOR. HIGHEST is last among listeners that may change drops. Do not use MONITOR to clear drops.

Skip if:

- `keepInventory` gamerule is true
- No items would be stored (all empty after exclude)
- Snapshot/spawn failed (then do not clear drops)

Otherwise:

1. Snapshot killer: `Player#getKiller()` UUID if another player
2. Copy storage/armor/offhand **except excluded slots**
3. Copy XP from the event (`getDroppedExp()`), then `setDroppedExp(0)`
4. `getDrops().clear()` so nothing hits the ground
5. Place chest + hologram + persist

PvP knockout (`PvpKnockoutManager` on `EntityDamageEvent` HIGHEST) means many fights never fire death. That is intended.

## Grave contents model

Use the same 41 logical slots as Thievery `PlayerSlotMap`:

- 0-35 storage (player storage contents; excluded indices stay null in the grave)
- 36-39 armor
- 40 offhand

Public API on `Grave` / `GraveManager` for Thievery:

- `getAt(Block)`
- `isOwner(UUID)`
- `getKiller()` (`UUID` or null)
- `isProtected()`
- `getItem(int logicalSlot)` / `setItem(...)`
- `flush()`
- `removeIfEmpty()` (block + hologram + file)

## Hologram

Per-viewer fake `TextDisplay` packets via ProtocolLib (`GraveVisualManager`), not a shared world entity. Billboard center. Line 1: `DisplayIdentityService.resolveCharacterName(victim)` (same as `%rpcharacters_name%`). Fallback: Minecraft name if no character.

Optional line 2 if `hologram-show-killer: true`:

- Killer is a player: `resolveCharacterName(killer)` then Minecraft name
- Else: damage cause name (`EntityDamageEvent.DamageCause`) or last damager type (death-time only; reload uses stored killer UUID)

Extra line for viewers who pass `GraveLootRules.canSteal`: configurable `messages.rob-hint` (default `Right click to rob`).

Player-facing text: hyphens only, never em dash (U+2014).

## Interact (no GUI)

Cancel `PlayerInteractEvent` right-click on a grave chest for everyone, then:

**Owner (or staff with a graves admin permission):**

- Add every remaining item to their inventory
- Overflow: `world.dropItemNaturally` at the grave
- Give stored XP
- Remove grave (block, hologram, file)

**Non-owner:**

- If `isProtected()` and clicker UUID is not `getKiller()`: message locked, do nothing
- Else: **do not** give items here. Thievery handles steal. If Thievery is missing, leave items in the grave (do not dump)

Cancel `InventoryOpenEvent` for grave chests so no vanilla chest GUI.

Cancel break, explode, piston, hopper `InventoryMoveItemEvent` for grave blocks (except owner recover path).

## Protection

`protected: true` when the victim had permission `rpchar.grave.protect` (default true for everyone, or config `protect-by-default: true`).

Killer UUID is stored even if protect is false (Thievery still uses it).

Do not unlock the grave for the world when the killer steals.

## Persistence

Gson files under `plugins/RPCharacters/graves/<uuid>.json`. Load all on enable, re-place holograms if the chest block is still a tagged grave. If the block is gone, drop contents at stored location and delete the file.

## Config (`graves.yml`)

```yaml
enabled: true
protect-by-default: true
hologram-show-killer: true
snapshot-interval-ticks: 20
expire-seconds: 0          # 0 = never
material: CHEST
hologram-offset-y: 1.2
excluded-slots:
  - 9
  - 10
  - 11
  - 12
```

Messages in the same file or `graves.messages.*`. Hyphens only.

## Thievery

Remove Maven AngelChestAPI, `plugin.yml` AngelChest depend, `AngelChestManager`, `AngelChestStealSource`. No remaining type names from that API.

Add `depend: [..., RPCharacters]` (RPC jar is already a systemPath in Thievery).

`GraveManager.isGrave(Block)` / `getAt` replaces the old block check in `ContainerManager`.

Steal:

- No steal GUI
- Config `graves.budget` (default pickpocket budget)
- Skip ignore rules and clue items
- Greedy take into thief inventory; **do not drop** thief overflow; stop when inventory or budget is full
- Remainder stays in the grave
- Debounce interact + any open attempt so one click cannot double-steal

Owner recover stays in RPC. Thievery must not steal from the owner.

## Deploy

Disable any other death-chest plugin on the live server so only RPC graves spawn.
