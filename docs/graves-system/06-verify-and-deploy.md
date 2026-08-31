# Batch 6 - Verify and deploy

## Tests

1. Walk land, jump in void: grave on last land, not in void.
2. Die with items in slots 9-12: those items are **not** in the grave. After respawn they are back in those slots.
3. Owner click: items in inv, overflow on ground, hologram gone.
4. Second player, locked, not killer: locked, items remain.
5. Killer click: stealable items taken up to budget/space; rest remain; no GUI.
6. Hopper under grave: no drain.
7. Restart server: grave + hologram still there.
8. Knockout PvP (non-lethal): no grave.

## Deploy

- Unload other death-chest plugins from the server `plugins` folder.
- Deploy RPCharacters then Thievery.
- Confirm `graves.yml` `excluded-slots` matches the live reserved UI (default 9-12).
