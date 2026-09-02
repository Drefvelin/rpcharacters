# Dev characters and realm wipes (batch plan)

**Lock:** [00-index.md](./00-index.md)

Do not start a later batch until the current batch's tests (or listed checks) pass. Implement from this file plus the lock, not from chat.

## Exit (must all be true)

- `dev-characters: true`: in-game finish tags and persists `dev`; **no** pending ingest, **no** applied ack, **no** kit-customise ingest, **no** roster push. Catalog sync still runs. A helper's website character does not appear in-game.
- `dev-characters: false`: new in-game characters are untagged; existing tagged rows stay tagged; pending ingest and roster (omit tagged) run.
- `/rpcharacter wipe website confirm` deletes this realm's site character tables via plugin key; meta/ranks remain.
- `/rpcharacter wipe tagged confirm` hard-deletes tagged local characters and best-effort site rows for those ids (when flag is false).
- Confirm TTL + same-sender; `rpcharacters.admin`; no em dash in chat.
- `/rpcharacter wipe website confirm` deletes this realm's site character tables via plugin key; meta/ranks remain.
- `/rpcharacter wipe tagged confirm` hard-deletes tagged local characters and best-effort site rows for those ids.
- Confirm TTL + same-sender; `rpcharacters.admin`; no em dash in chat.

---

## Batch 1 - Flag persist

**Repo:** `rpcharacters`

- `config.yml`: `dev-characters: false` plus a short comment (jar default false).
- `Cache.devCharacters` + `ConfigLoader`.
- `RPCharacter` boolean `dev`, getter/setter.
- `Database.saveCharacter` / load: JSON `dev` (write only when true; read missing as false).

**Check:** Create a character in-game with flag false, JSON has no `dev`. Manually set `"dev": true`, reload, flag reads true.

---

## Batch 2 - Isolate when flag on (tag, skip character sync)

**Repo:** `rpcharacters`

- `CharacterCreation` finish: `if (Cache.devCharacters) character.setDev(true)` immediately before `addCharacter`.
- If `Cache.devCharacters`: `CharacterIngestService` pull/force-pull/join pull no-ops (no GET pending, no apply, no ack). `KitCustomiseIngestService.pullNow` no-op. `RosterSyncService.pushRosterNow` / async helpers return without HTTP.
- If flag false: ingest unchanged; roster omits `isDev()` characters. Empty keeper list still replace-pushes.

**Check:** Flag on: website pending stays pending; staff in-game char tagged and not on site. Flag off: pending ingest applies; helper sees the website character in-game.

---

## Batch 3 - Local hard-delete helper

**Repo:** `rpcharacters`

New domain helper (e.g. `ingest` or `wipe` package, one class): scan `data/characterdata`, delete tagged (or a given id list), online vs offline, freeze/mail/kit, save, roster push **only if flag is false**, return `{playersTouched, charactersDeleted, deletedIds}`.

Do not use `PermadeathService.killCharacter`. Delete the JSON file.

**Check:** Two characters one tagged: helper removes only tagged file; active tagged player is no longer on that character; mail directory drops the id.

No command yet.

---

## Batch 4 - ProvinceSystem realm wipe endpoint

**Repo:** `ProvinceSystem`

- `DELETE /characters/plugin/realm-data` (or `POST .../wipe` with empty body). `require_plugin_key`. `realm_id` query/header same as `/plugin/pending`.
- Delete the five tables in the lock for that realm. Return counts.
- Unit test: two realms, wipe `main`, `dev` rows remain; plugin key required; `rpc_player_meta` untouched.

No RPCharacters client yet.

---

## Batch 5 - ProvinceSystem delete-by-character-ids

**Repo:** `ProvinceSystem`

- Plugin-key `POST /characters/plugin/characters/delete` body `{ "character_ids": ["..."] }` scoped to injected `realm_id`.
- Delete roster + lore + wardrobe rows for those ids in that realm. Do **not** delete `character_creates` (donors).
- Test: tagged id gone from roster; other character on same player remains.

Used by tagged wipe after local delete (leaked mirrors).

---

## Batch 6 - Plugin client + commands

**Repo:** `rpcharacters`

- `ProvinceSystemClient`: DELETE realm-data, POST delete-by-ids.
- Pending map: sender + action (`website` | `tagged`) + expiry 30s.
- `CommandManager` + `CommandTabCompleter`: `wipe` → `website` | `tagged`; second arg `confirm` only after first step exists (tab can always offer `confirm`).
- Permission `rpcharacters.admin`. Console allowed.
- `wipe website confirm`: client realm wipe; chat success with counts or API error.
- `wipe tagged confirm`: Batch 3 helper; then Batch 5 with `deletedIds` (fail-soft log).

Chat examples: `Type /rpcharacter wipe website confirm within 30 seconds.` `Wiped website character data for realm main.` `Deleted 4 tagged character(s).`

**Check:** Non-admin denied. Confirm without prelude: `Nothing to confirm.` Wrong action confirm ignored. Expired: must start over.

---

## Batch 7 - Verify

- Pre-season box: `dev-characters: true`, Noble web lock unchanged, catalog still syncs.
- Staff in-game create → tagged → not on site (no roster). Helper website create → stays pending → **not** in plugin until flag is false.
- `wipe tagged` does not delete website pending. `wipe website` on realm `main` **does** delete those pending rows; do not use it until you mean to throw away `main` site data.
- After migrate / flag false: pending ingest applies website characters; tagged local tests gone if wiped.
- Search changed player-facing files for U+2014: zero matches.

---

## Out of scope

- Wipe on enable
- Tag backfill
- Donator gate for in-game create
- `web-creator.yml` edits
