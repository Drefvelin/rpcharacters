# Batch 5 - Thievery hook

RPC does not import Thievery. Public grave accessors only.

## Thievery changes

- Delete `steal/AngelChestManager.java`, `steal/source/AngelChestStealSource.java`
- Remove AngelChestAPI from `pom.xml` and `depend` in `plugin.yml`
- Add RPCharacters to `depend` if not already a hard depend (already systemPath)
- New `steal/GraveManager` listener name: `GraveStealListener` (avoid clashing with RPC `GraveManager`)
- `steal/source/GraveStealSource.java` - same 41-slot mapping as `PlayerSlotMap`
- `config.yml` key `graves.budget` (not a third-party plugin name)
- `ContainerManager` skip when RPC grave is at the clicked block

## Steal behaviour

No GUI. One greedy pass. Budget + `StealTakeHandler.maxFitInPlayerInventory`. Stop when full. Do not drop leftover stealable items. Clues and ignore rules skipped. Killer bypasses lock; owner never stolen from.

## Done when

Thievery compiles without any death-chest API; killer can steal from a locked grave; non-killer cannot; owner recover still works via RPC.
