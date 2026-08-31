# Batch 2 - Grave model, PDC, persist

## Files

- `grave/Grave.java` - owner, killer, protected, created, experience, storage/armor/offhand, hologram UUID, block location
- `grave/GraveManager.java` - `Map` by block key, `getAt`, save/load Gson, PDC keys on chest
- `grave/GraveKeys.java` or namespaced keys on `RPCharacters.plugin`

## PDC

Chest block (or tile entity) tagged so lookup works after restart before Gson matches.

## Done when

Save/load round-trip of items + killer UUID + excluded slots stored as empty.
