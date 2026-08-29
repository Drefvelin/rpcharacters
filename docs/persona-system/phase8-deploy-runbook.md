# Phase 8 — Deploy runbook

Operational checklist for retiring OpenRP persona modules, ConditionalEvents rolls, and the Thievery mask layer. RPCharacters Phases 1–7 are the single source of truth.

**Code complete in repo:** Thievery mask package removed; mask detection and masked chat/profile live in RPCharacters only.

---

## Pre-deploy builds

1. `mvn package` (or `mvn compile`) in `rpcharacters` — jar includes persona, chat, masks, profile, rolls, calendar.
2. `mvn package` (or `mvn compile`) in `thievery` — jar must **not** register `MaskChatListener` or `MaskProfileBlockListener`.
3. On existing servers: remove `masks` and `masked-channels` from `plugins/Thievery/config.yml` if the file was copied from an older default (new jar default no longer includes them).

---

## Staging — plugin jars

- [ ] Deploy latest **RPCharacters** jar
- [ ] Deploy **Thievery** jar (mask-free build from Phase 8)
- [ ] **OpenRP:** remove jar entirely, or disable all replaced modules in `enabled:` — `chat`, `descriptions`, `rolls` (see reference `orp/config.yml`). OpenRP time was not ported; remove if unused.
- [ ] **ConditionalEvents:** disable or remove events from `a_rolls.yml` (reference: `orp/a_rolls.yml`) - `/roll` and attribute modifier rolls are native in RPCharacters
- [ ] Confirm no other plugin registers a conflicting `/roll` command

**Order:** Disable OpenRP chat **before** relying on RPCharacters chat on staging. Do not run OpenRP chat + RPCharacters chat together.

---

## Staging — RPCharacters configs

Ensure these exist under `plugins/RPCharacters/` (copy from jar resources on first run or merge manually):

- [ ] `persona.yml` — set `no-character-fallback: '§f§oUnknown'` if upgrading from an older file (bundled default in new jar; `createConfigs()` does not overwrite existing files)
- [ ] `chat.yml` — ensure `no-character-message` and per-channel `require-character: false` on OOC/staff channels (`ooc`, `admin`, `helper`, `dm`); IC channels default to requiring a character
- [ ] `masks.yml`
- [ ] `rolls.yml`
- [ ] `profile-view.yml`
- [ ] `permission-groups.yml` — donor/default perks (name-colour stops, character-switch cooldown days)
- [ ] `calendar.yml` — set `base-irl-year` to the IRL year when the season started; add `age.minimum: 18` if upgrading (global minimum character age)
- [ ] `races.yml` — remove obsolete per-race `age-min` keys if present (only `age-max` per race is used now)

Reload or restart after config changes.

---

## Staging — LuckPerms

Assign `rpchar.*` per phase0-design §11:

- [ ] `rpchar.persona.set` — `/rpcharacter alias`, gender, description
- [ ] `rpchar.group.noble` / `rpchar.group.gilded` / `rpchar.group.ascended` — name-colour stops and shorter switch cooldown (see `permission-groups.yml`)
- [ ] `rpchar.profile` — shift-right-click profile
- [ ] `rpchar.chat.use` (+ channel-specific perms from `chat.yml`)
- [ ] `rpchar.roll` (+ `rpchar.roll.alt` if using 1–200 `/roll`)
- [ ] Admin: `rpchar.persona.override`, `rpchar.chat.admin`, etc. as needed
- [ ] Staff: `rpchar.tempalias` (session IC chat override), `rpchar.character.hidden` (hide char from TAB via slug)

Retire unused `orpdesc.*` groups if migrating permission templates.

---

## Staging — TAB / PlaceholderAPI

- [ ] Tab list name: **`%rpcharacters_display_safe%`** only (never `%rpcharacters_display%` on TAB)
- [ ] `%rpcharacters_display_no_mask%` is the **real active** character (profiles, character menus) — not TAB when hidden chars are used
- [ ] Remove `%orpdesc_*%` from TAB, scoreboards, and any remaining formats
- [ ] Chat/profile placeholders: `%rpcharacters_name%`, `%rpcharacters_display%`, `%rpcharacters_display_safe%`, `%rpcharacters_age%`, `%rpcharacters_race%`, `%rpcharacters_gender%`, `%rpcharacters_description%`

---

## Staging smoke test

### Chat

- [ ] Channels: `rp`, `shout`, `yell`, `whisper`, `looc`, `ooc`, `action`, `admin`, `helper`, `dm`
- [ ] Plain chat (no command) → default `rp` channel
- [ ] No active character: plain chat and IC channels blocked; TAB shows **Unknown** (`%rpcharacters_display_safe%`); `/ooc` works with IGN
- [ ] Channel commands registered from `chat.yml` on load/reload (no jar rebuild for new channels)
- [ ] Colour codes gated by `rpchar.chat.colors`
- [ ] Channel cooldowns work
- [ ] **No duplicate messages** (only RPCharacters handling chat)

### Masks

- [ ] Wearing mask: `{display}` channels show plain **Masked** (no colour on label)
- [ ] Plain chat (no command) works while masked — shows **Masked** on default RP channel (no `/rp` required)
- [ ] TAB name unchanged when mask worn (`display_safe` ignores mask; chat `{display}` shows **Masked**)
- [ ] LOOC/OOC still use account `{player}` — mask does not rename those channels

### Profile

- [ ] Shift-sneak, empty hand, right-click player → profile lines from `profile-view.yml`
- [ ] **Denied** when target is masked
- [ ] `/rpcharacter profile [player]` works with `rpchar.profile`

### Persona

- [ ] `/rpcharacter alias`, `namecolour`, `gender`, `description`
- [ ] `/rpcharacter tempalias <name>|clear` — session IC chat override (below mask); cleared on quit
- [ ] `/rpcharacter sethidden <slug>` toggle; `/rpcharacter sethidden <slug> clear` to unhide; TAB uses `%rpcharacters_display_safe%`
- [ ] Name colour: tier limits from `permission-groups.yml` (0 default, 1 noble, 2 gilded, 20 ascended); multi-hex gradient supported
- [ ] Admin `/rpcharacter override ... namecolour` bypasses tier limits and persists through rank changes (`name-colour-staff`)
- [ ] Default description template with `{continent}` — vowel races produce **An** (not **Aan**)
- [ ] Character Info GUI (ender pearl) shows display, gender, age, and description

### Character switch cooldown

- [ ] Stored as `last-character-switch-ms` timestamp (wall-clock; no online-only tick)
- [ ] Default **14** days; noble **10**, gilded **7**, ascended **5** (`permission-groups.yml`)
- [ ] Cooldown length resolved from **current** rank at check time (upgrade mid-cooldown can unlock sooner)
- [ ] Legacy `cooldown` minutes in player JSON migrated on first load
- [ ] `/rpcharacter skipcooldown <player>` clears switch timestamp

### Rolls

- [ ] `/roll` → 1–100 (or 1–200 with `rpchar.roll.alt`)
- [ ] `/roll 20` and `/roll 20 +3` (display-only modifier)
- [ ] `/roll strength` (or other attribute) with MMOCore modifier

### Age

- [ ] Character creation: age stage after race selection; birthday saved in character JSON
- [ ] Minimum age **18** for all races (`calendar.yml` `age.minimum`); maximum per race (`age-max` in `races.yml`)
- [ ] `%rpcharacters_age%` and profile `{age}` show computed age; `Unset` when no birthday

### Conversations

- [ ] `ConversationManager` counts via `CharacterChatEvent` only
- [ ] Plain chat counts as `rp`; LOOC excluded; masked speakers skipped for conversation tracking

---

## Production cutover

Use a **brief maintenance window** — Thievery mask layer and OpenRP chat must not run alongside RPCharacters during cutover.

1. [ ] Announce maintenance
2. [ ] Stop server
3. [ ] Deploy **RPCharacters** jar (Phases 1–7)
4. [ ] Deploy **Thievery** jar (mask-free)
5. [ ] Remove/disable **OpenRP** (or all replaced modules)
6. [ ] Disable **ConditionalEvents** roll events (`a_rolls.yml`)
7. [ ] Verify LuckPerms `rpchar.*` and TAB `%rpcharacters_display_safe%` on production configs
8. [ ] Start server
9. [ ] Repeat staging smoke test (section above)
10. [ ] Monitor first session for double-chat or wrong TAB names

---

## Risks

| Risk | Mitigation |
|------|------------|
| Dual chat plugins | Disable OpenRP chat before enabling RPC chat |
| Thievery mask + RPC mask both active | Deploy Thievery Phase 8 build in same window as RPC go-live |
| Wrong TAB placeholder | Use `%rpcharacters_display_safe%` only |
| Lost `/chtsw` | Optional Phase 9; not required for go-live |

---

## Optional Phase 9 (not in this deploy)

- Channel toggle/switcher (`/chtsw`) — reference `orp/chat/toggle-and-switcher.yml`
- Action-channel `*` emphasis in `/me`

---

## Reference only (do not deploy)

- `rpcharacters/orp/` — exported old server configs for comparison
- `rpcharacters/orp/a_rolls.yml` — ConditionalEvents roll events to disable
