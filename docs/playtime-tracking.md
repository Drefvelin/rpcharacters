# Playtime tracking

Real time a character has spent **online**, accumulated per character. Added so SimpleFactions can pay faction prestige for member engagement rather than for headcount alone.

**Counter:** `Objects/RPCharacter.onlinePlaytimeSeconds` - **Service:** `playtime/PlaytimeService` - **Index file:** `Database/PlaytimeIndexDatabase`

---

## Three notions of "playtime", only one of which is real

The codebase already used the word for two things that are not online time. Keep them apart.

| Name | What it measures | Used by |
|------|------------------|---------|
| `RPCharacter.getAgeSeconds()` | Wall-clock seconds since the character was created. Climbs while the player is offline | Character age readouts |
| `PlayerData.getAgeSeconds()`, via `Utils/PlaytimeGate` | Wall-clock account age, despite the class name and the `required-account-playtime-seconds` trait key | Trait selection requirements |
| `RPCharacter.getOnlinePlaytimeSeconds()` | Seconds the character was actually online | Faction prestige |

## How it accrues

`PlaytimeService.startTicks()` runs every 60 seconds over online players, and credits 60 seconds to the **active** character. Consequences worth knowing:

- Playtime is per character. Switching characters switches which counter grows.
- A player with no active character (mid-creation, or awaiting a permakill respawn) accrues nothing and reads as 0, so a dead character stops earning immediately.
- Partial minutes at logout or shutdown are dropped. Nothing tries to reconcile them.

## Persistence

Two files, on purpose.

**Per character**, in the character's own JSON, alongside every other character field:

```
"online-playtime-seconds": 12600
```

> The existing `playtime-seconds` key is **not** this. `DurationParser.resolveCreatedAtEpochSeconds` reads it as a legacy epoch-timestamp fallback for `created-at`, so writing an accumulating count there would corrupt character ages. The new key exists precisely to avoid that collision.

**A flat index** at `data/playtime-index.json`, one entry per player:

```json
[ { "uuid": "…", "name": "Steve", "seconds": 12600 } ]
```

The index is the read surface. Callers ask for a player by name and get an answer from an in-memory map with no disk access, which per-character files cannot offer because they are only read at login and `PlayerData` is dropped from memory on quit. Entries are keyed by uuid so a rename overwrites cleanly on the next login; a lowercase-name map is derived for lookups.

Loaded in `onEnable`, saved by a dirty-flag autosave every 200 ticks and again on `onDisable`, mirroring `SpawnedClueDatabase`.

**Offline players are not stale.** Online playtime cannot advance while a player is away, so a cached figure for an offline player is exact, not approximate. That is what makes an index safe to hand to a caller on a hot path.

## Reading it from another plugin

```java
Integer seconds = PlaytimeService.getSeconds("Steve");   // or getSeconds(UUID)
```

`null` means the player is unknown to the index, which is **not** the same as zero: a player who has never been seen differs from one with a fresh character. Callers should decide which of the two they want rather than defaulting `null` to 0.

`PlaytimeService.refresh(Player)` republishes a player's figure without crediting time. `PlaytimeListener` already calls it on join and on `CharacterActivatedEvent`, so a character switch shows up at once instead of trailing the tick by up to a minute.

## Consumer

SimpleFactions faction prestige. See `simplefactions/docs/prestige.md` for the curve, the cap and the probe seam it reads through.
