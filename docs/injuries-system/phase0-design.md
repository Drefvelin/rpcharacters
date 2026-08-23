# Phase 0 — Injuries, Healing, Prosthetics Design Lock

**Status:** Design spec  
**Target:** RPCharacters (`net.tfminecraft.RPCharacters`)  
**Context:** Replaces flat injury traits (`one_handed`, `one_legged`, etc.) with healing vs permanent injuries, remedies, prosthetic replacements, and optional fuel.

---

## 1. Goal

- Injuries are traits with optional **healing** (`duration`) or **permanent** (no duration).
- Permadeath zone death: roll permakill first, then either **convert** healing injuries to permanent (conversion **replaces** a new pool roll) or **roll** a new healing injury from the pool.
- Remedies instantly remove **healing** injuries only.
- Prosthetics replace permanent injuries via config mapping; tiered upgrades; optional fuel on advanced tiers.
- Character creator: optional backstory stages for permanent injuries (0 cost, many) and one prosthetic (1 point budget, skippable).
- Web character creator mirrors in game rules.

---

## 2. Death flow (permadeath zone)

```
1. Roll permadeath (injuryCount × chance-per-injury)
2. If permakilled → stop
3. If character has any healing injury traits:
     convert each via injury-progression map (healing trait id → permanent trait id)
     (conversion is this death's injury consequence; no pool roll)
4. Else:
     pick random new healing injury from injuries.yml pool
```

**Rules:**
- Permadeath chance = count of traits with `key: injury` (healing + permanent). Prosthetics (`key: prosthetic`) do not count.
- Converting healing → permanent does not change injury count.
- Multiple healing injuries on one death: convert all of them; still no pool roll.
- Potion effects on injuries do **not** scale during healing. Attribute penalties scale proportionally (ints).

---

## 3. Healing

- Trait YAML may define `duration` (e.g. `48h`). Character stores `duration-remaining-ms` in `trait-state`.
- Ticks only while owner is **online** and injury is on the **active** character.
- Attribute effect at progress `p` (0 = just gained, 1 = healed):  
  `effective = round(fullPenalty × (1 - p))` toward zero.
- At `duration-remaining-ms <= 0`: remove trait, apply lost message, refresh integrator.
- Example progression: `half_blind` (healing) → `blind` (permanent) on zone death via progression map. `half_blind` can also heal away over time.

---

## 4. Remedies

- **Option A (locked):** drinking a remedy item instantly removes a matching **healing** injury (trait has `duration` in YAML). Permanent injuries ignored.
- Existing remedy listener pattern; filter by healing flag / duration presence.

---

## 5. Prosthetics

- `prosthetics.yml`: abstract `replacements` map: **permanent injury trait id** → install item + ordered **tier** trait ids.
- Right click with install item: install or upgrade prosthetic for mapped injury.
- Installing removes the permanent injury trait and adds the prosthetic trait.
- Upgrading swaps prosthetic trait id to higher tier (same mapping entry). Preserve fuel **percentage** on upgrade; fresh install starts at full fuel.
- No prosthetic entry in config = no replacement path (e.g. blindness). Not hardcoded.
- Placeholder install item for all tiers: `v.blaze_rod` (config).

### Fueled prosthetics (arcane tiers)

- `fuel-templates.yml`: reusable templates (`item`, `amount-per-item`, `burn-rate`, `burn-interval`).
- Prosthetic trait references `fuel-template`, `fuel-capacity`.
- Character `trait-state` stores `fuel` double.
- Same trait id when depowered; swap **name**, **description**, **attribute-modifiers**, **potion-effects** from YAML `powered` / `depowered` blocks.
- Refuel: right click with fuel item (VehicleFramework style). Arcane: `m.miscellanea.arcane_fuel`, +50 fuel, capacity 50, burn 1 per hour.
- Creator pick: start at full fuel; disclaimer in stage info and trait lore.

---

## 6. Trait keys

| Key | Use |
|-----|-----|
| `injury` | Healing and permanent injuries |
| `prosthetic` | Replacements |

Creator stages filter by key. Healing injuries identified by `duration` on trait definition. Permanent pickable injuries have no `duration`.

---

## 7. Character JSON

```json
"traits": ["broken_arm", "wooden_claw_arm"],
"trait-state": {
  "broken_arm": { "duration-remaining-ms": 172800000 },
  "arcane_prosthetic_arm": { "fuel": 50.0 }
}
```

**Load migration:** missing `trait-state` → `{}`. For each owned trait:
- healing (`duration` in YAML): missing `duration-remaining-ms` → full duration from trait def.
- fueled prosthetic: missing `fuel` → full capacity.

**Legacy trait ids:** migrate on load (`one_handed` → `broken_arm`, etc.) per migration table in config or code.

---

## 8. Config files

| File | Purpose |
|------|---------|
| `injuries.yml` | Weighted pool of **healing** trait ids for new zone rolls |
| `injury-progression.yml` | `healing-trait-id: permanent-trait-id` |
| `fuel-templates.yml` | Fuel item templates |
| `prosthetics.yml` | Permanent injury → tiers + install item |
| `traits/injury-traits.yml` | Injury trait definitions |
| `traits/prosthetic-traits.yml` | Prosthetic trait definitions |
| `items.yml` | Remedies (healing traits only) |

---

## 9. Character creator

Two new stages (skippable, like evil):

| Stage | Key | Points | min/max | Notes |
|-------|-----|--------|---------|-------|
| Permanent injuries | `injury` traits without `duration` | 0 | 0 / many | Optional backstory |
| Prosthetic | `prosthetic` | 1 | 0 / 1 | One prosthetic max; injury not required |

Clear icons on all trait options. Arcane prosthetic disclaimer about fuel.

---

## 10. Web sync

- `CreationCatalogSyncService`: include permanent injuries and prosthetics; exclude runtime healing injuries.
- Match in game point rules and skip behavior.

---

## 11. GUI copy

- No em dashes (U+2014).
- Avoid normal hyphens in player facing strings unless required for formatting; prefer commas and spaces.

---

## 12. Balance intent

- Prosthetic tiers: gradually better than permanent injury, never as good as uninjured.
- All injuries count equally for permadeath risk until replaced by prosthetic (injury removed).
