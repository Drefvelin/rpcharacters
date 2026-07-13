# Phase 8 — Professions Deploy Runbook

## Pre-deploy

- [ ] Build RPCharacters with integrated professions module
- [ ] Split profession YAMLs present under `plugins/RPCharacters/professions/`
- [ ] `plugins/RPCharacters/professions.yml` global settings reviewed
- [ ] Archive `plugins/Professions/PlayerData/` (reference only — greenfield, no auto-migration)

## Deploy steps

1. Stop server (or hot-swap during maintenance window)
2. Deploy new `RPCharacters.jar`
3. **Remove** `Professions.jar` from `plugins/`
4. Start server — default `professions.yml` + `professions/*.yml` copy from jar if missing
5. Verify `plugins/RPCharacters/professions/` contains all 7 profession files

## Post-deploy verification

- [ ] `/profession` opens main menu on a character with active RP character
- [ ] MMOCore profession level-up grants lifetime points (account-wide)
- [ ] Purchasing upgrade on Character A spends from A's loadout; Character B still has full free pool
- [ ] Character switch strips Character A `professions.*` LuckPerms; Character B perks apply
- [ ] Crafting station permission checks still work (`professions.*` recipe conditions)
- [ ] Breeding lock/unlock behaves as before
- [ ] `/profession top <prof>` lists online players
- [ ] `/profession restoreall` / `fixperms` if LP nodes look wrong after first login

## External configs

- AdvancedCrafting and other plugins using `permission-prefix: professions.` — **no change needed**
- MMOCore profession IDs must match `professions/*.yml` filenames

## Rollback

1. Restore previous `RPCharacters.jar`
2. Restore `Professions.jar`
3. Remove integrated profession configs if desired (optional)

## Notes

- Greenfield deploy: players re-earn lifetime points from MMOCore levels on first join bootstrap
- Use `/profession givepoints` for launch grants if needed
