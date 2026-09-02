# Batch 3 - Death spawn and hologram

## Files

- `grave/GraveDeathListener.java` - `PlayerDeathEvent` **HIGHEST**
- `grave/GraveVisualManager.java` - per-viewer fake `TextDisplay` packets via ProtocolLib (not a shared world entity)
- `grave/GraveHologramTexts.java` - victim name, optional killer line, rob hint when `GraveLootRules.canSteal`

## Copy rules

Copy from `PlayerInventory` by slot. Skip `excluded-slots`. Stash those stacks and restore them on respawn. Then clear `event.getDrops()` and zero dropped XP after storing XP.

## Names

`DisplayIdentityService.resolveCharacterName` for victim and player killer. Not mask, not `%rpcharacters_display%`.

## Done when

Dying in lava/void places a chest on last solid ground with per-viewer hologram text; excluded slots are not in the chest; other drops do not appear on the ground.
