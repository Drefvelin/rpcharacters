# Phase 0 — RP Persona System Design Lock

**Status:** Design spec (documentation only — no runtime changes in Phase 0)  
**Target plugin:** RPCharacters (`net.tfminecraft.RPCharacters`)  
**Context:** New season greenfield — no data migration, no legacy placeholders, no backwards compatibility.

---

## 1. Goal

Produce a single authoritative design document before any Phase 1 code. Phase 0 locks:

- Data model on `RPCharacter`
- PlaceholderAPI contract (name / display / display_no_mask)
- `/char` command tree
- Bukkit events API
- Config file layout
- Permission nodes (`rpchar.*`)
- Feature checklist
- Deploy checklist

**Phase 0 does not:** change Java, `pom.xml`, or live server configs.

---

## 2. System scope

RPCharacters becomes the single source of truth for active-character persona and RP social features:

| Feature | Owner | Phase |
|---------|-------|-------|
| Persona fields (alias, colour, gender, description) | RPCharacters | 1–2 |
| PlaceholderAPI expansion (`rpcharacters`) | RPCharacters | 2 |
| Masks (helmet item → anonymized chat) | RPCharacters | 3 |
| RP chat channels | RPCharacters | 4 |
| Shift-right-click profile | RPCharacters | 5 |
| Dice rolls (+ MMOCore attribute modifiers) | RPCharacters | 6 |
| Dynamic age / calendar | RPCharacters | 7 |
| Character CRUD, traits, clues, creation | RPCharacters | existing |

### 2.1 Command ownership

| Command | Purpose |
|---------|---------|
| `/char` | Active character persona (alias, colour, gender, description, profile) |
| `/rp`, `/shout`, `/yell`, `/whisper`, `/me`, `/looc`, `/ooc`, staff channels | RP chat (Phase 4) |
| `/roll`, `/tfmc roll` | Dice rolls (Phase 6) |
| `/rpcharacter` | Character management — create, switch, traits, clues |

### 2.2 Current coupling to remove

| File | Remove |
|------|--------|
| `RPCharacter.java` | `modify("name")` → `dispatchCommand("char set name")` |
| `RPCharacter.java` | `activate()` / `deactivate()` external persona sync |
| `MaskChecker.java` | Reflection into Thievery |
| `ConversationManager.java` | Chat preprocess hacks — replace with `CharacterChatEvent` (Phase 4) |
| `thievery/mask/*` | Duplicate masked channel formats (Phase 8 deploy) |

### 2.3 Chat architecture

One `CharacterChatEvent` with mutable `displayName` + recipient set, fired before send. Mask override applied in `DisplayIdentityService` — one format per channel, no duplicate masked-channel configs.

---

## 3. Locked decision: `/char` command tree

Use **`/char alias`** — not `set name`, not `displayname`. The real character name (`RPCharacter.name`) is never changed by alias.

| Command | Behaviour |
|---------|-----------|
| `/char alias <name>` | Set temporary RP label; **strip all `§` / `&` colour codes** |
| `/char alias clear` | Clear alias; effective display falls back to `character.name` |
| `/char namecolour <#hex>` | Single colour on effective name (alias ?? name) |
| `/char namecolour "<#a> <#b>"` | Gradient between two hex values |
| `/char namecolour clear` | Remove stored colour |
| `/char gender <value>` | From allowed list in `profile.yml` |
| `/char description <text>` | Optional custom description |
| `/char description clear` | Revert to default template |
| `/char profile [player]` | Optional command mirror of shift-right-click view |

`/rpcharacter` stays for account/character management.

**Colour rule:** alias text never carries colour. Colour is **only** via `namecolour`.

---

## 4. PlaceholderAPI contract

Expansion identifier: **`rpcharacters`**

### 4.1 Name placeholders (complete set)

Only three name-related placeholders are needed:

| Placeholder | Value | Mask? | Colour? | Primary use |
|-------------|-------|-------|---------|-------------|
| `%rpcharacters_name%` | `RPCharacter.name` (creation identity) | n/a | no | Admin, logs, character sheet |
| `%rpcharacters_display%` | `(alias ?? name)` + colour/gradient; if masked → plain `masked-label` | **yes** | only when unmasked | **Chat** `{display}`, `/me` |
| `%rpcharacters_display_no_mask%` | `(alias ?? name)` + colour/gradient | **never** | **yes** | **TAB**, scoreboards, nametags |

### 4.2 Design principles

1. **Masked labels are always plain text, never coloured.** Colour applies only to known personas.
2. **TAB uses `%rpcharacters_display_no_mask%` only** — never `%rpcharacters_display%`. Tab list stays stable when a mask is worn; chat anonymizes separately.
3. **Single resolver** — `DisplayIdentityService`; placeholders and chat `{display}` token call the same methods.

### 4.3 Resolution pipeline

```
activeCharacter = PlayerManager.getActiveCharacter(player)
effectivePlain  = stripColour(alias ?? character.name)

%rpcharacters_name%              = character.name
%rpcharacters_display_no_mask%     = applyNameColour(effectivePlain)
%rpcharacters_display%             = isMasked ? plainMaskedLabel : applyNameColour(effectivePlain)
```

**No active character:** all placeholders return empty string (or configurable fallback — decide in Phase 2).

### 4.4 Profile placeholders (non-name)

| Placeholder | Value |
|-------------|-------|
| `%rpcharacters_race%` | Active character race display name |
| `%rpcharacters_gender%` | Gender field or default |
| `%rpcharacters_age%` | Static until Phase 7 calendar; then computed from birthday |
| `%rpcharacters_description%` | Custom description or default template |

### 4.5 Chat format inline tokens (`chat.yml`)

| Token | Resolver |
|-------|----------|
| `{display}` | `resolveDisplay(player)` — masked path |
| `{display_no_mask}` | `resolveDisplayNoMask(player)` — staff channels if needed |
| `{message}` | Sanitized message body |
| `{player}` | Bukkit account name (LOOC/OOC/admin channels) |

LOOC/OOC/admin channels use `{player}` (account name), not `{display}` — masks do not affect those channels.

---

## 5. DisplayIdentityService

Package: `net.tfminecraft.RPCharacters.identity`

```java
// Pseudocode — implementation spec for Phase 1+
public final class DisplayIdentityService {

    /** Creation identity; never affected by alias or mask. */
    public static String resolveCharacterName(Player player);

    /** (alias ?? name) + colour/gradient; NEVER masked. TAB / %rpcharacters_display_no_mask%. */
    public static String resolveDisplayNoMask(Player player);

    /** Masked plain label, or resolveDisplayNoMask when unmasked. Chat / %rpcharacters_display%. */
    public static String resolveDisplay(Player player);

    public static boolean isMasked(Player player);
}
```

Colour application uses TLibs `StringFormatter.formatHex`. Gradient algorithm locked in Phase 2 implementation.

---

## 6. Data model on `RPCharacter`

New persisted fields in character JSON (`plugins/RPCharacters/data/characterdata/<uuid>/<char-id>.json`):

| JSON key | Java field | Type | Notes |
|----------|------------|------|-------|
| `alias` | `alias` | `String` | nullable; temp RP label |
| `name-colour` | `nameColour` | `NameColour` | solid or gradient |
| `gender` | `gender` | `String` | nullable |
| `description` | `description` | `String` | nullable; empty → template |
| `birthday` | `birthday` | ISO date string | Phase 7; nullable until then |

**Not persisted on character:** mask state (derived live from helmet slot).

### 6.1 Activate / deactivate behaviour

| Event | Behaviour |
|-------|-----------|
| `activate()` | Expose persona via placeholders; remove external persona dispatch (Phase 1) |
| `deactivate()` | Placeholders return empty for that player |
| `setAlias()` | **Never** writes to `character.name` |

### 6.2 Default description template

```yaml
# config.yml
continent: Cerrith

# profile.yml
description-default: "A{n} {race} in {continent}."
```

`{n}` → `a` or `an` based on race name. If `description` is null/blank, `%rpcharacters_description%` and profile view use this template.

---

## 7. Events API

### 7.1 `CharacterChatEvent`

- **Timing:** async ingestion; sync send on main thread before delivery
- **Implements:** `Cancellable`

| Field | Type | Mutable? | Notes |
|-------|------|----------|-------|
| `sender` | `Player` | no | |
| `character` | `RPCharacter` | no | active character |
| `channel` | `String` | yes | channel id e.g. `rp` |
| `message` | `String` | yes | after colour-strip per permission |
| `displayName` | `String` | **yes** | default `resolveDisplay(sender)` |
| `recipients` | `Set<Player>` | yes | range-filtered |
| `masked` | `boolean` | no | |
| `wasCommand` | `boolean` | no | |

**Listener priorities:**

| Listener | Priority | Notes |
|----------|----------|-------|
| `ChatManager` (internal) | `LOWEST` | ingest, format, send |
| Other plugins (optional) | `NORMAL` | may override `displayName` |
| `ConversationManager` | `MONITOR` | count conversations; ignore cancelled |

### 7.2 `CharacterProfileViewEvent`

- **Timing:** sync
- **Implements:** `Cancellable`

| Field | Type | Notes |
|-------|------|-------|
| `viewer` | `Player` | |
| `target` | `Player` | |
| `targetCharacter` | `RPCharacter` | |
| `masked` | `boolean` | if true, default handler denies |

Default handler cancels when: masked, no `rpchar.profile` permission, not sneaking, hand not empty, no active character on target.

### 7.3 Optional events (Phase 2+)

| Event | Purpose |
|-------|---------|
| `CharacterAliasChangeEvent` | TAB refresh hint |
| `CharacterMaskChangeEvent` | Helmet slot change |

Not required if TAB polls PlaceholderAPI on its normal refresh cycle.

---

## 8. Masks owned by RPCharacters

`masks.yml`:

```yaml
masked-label: "Masked"   # plain text, no colour — used by resolveDisplay

masks:
  ghost_mask:
    item: m.masks.ghost_mask
```

**Design:** one chat format per channel; `{display}` becomes plain `Masked` when helmet matches a mask item path (TLibs item checker).

### 8.1 Thievery cleanup (deploy)

- Delete `thievery/mask/*` package
- Remove `MaskLoader` from Thievery `ConfigLoader`
- Remove `masked-channels` and `masks` from thievery `config.yml`
- Unregister `MaskChatListener`, `MaskProfileBlockListener`

### 8.2 RPCharacters cleanup (Phase 3)

- Delete `Utils/MaskChecker.java`
- Remove `softdepend: [Thievery]` from `plugin.yml`

**Dependency direction:** RPCharacters never imports Thievery.

---

## 9. Config file layout

| File | Purpose |
|------|---------|
| `config.yml` | Existing settings + `continent` string |
| `profile.yml` | Gender list, validation, description template, profile format |
| `chat.yml` | Channel defs, formats, ranges, permissions |
| `masks.yml` | Mask items + `masked-label` |
| `rolls.yml` | Roll ranges, broadcast format, MMOCore attribute modifier table |

Phase 4: `chat.yml` becomes source of truth for channel ranges; merge with existing `conversation.channels` in `config.yml`.

---

## 10. Config drafts

### 10.1 `profile.yml`

```yaml
use-perm: rpchar.persona.set
override-perm: rpchar.persona.override
profile-perm: rpchar.profile
bypass-cooldown-perm: rpchar.persona.bypasscooldown
namecolour-perm: rpchar.namecolour

alias:
  length:
    minimum: 3
    maximum: 24
  allowed-chars: "abcdefghijklmnopqrstuvwxyz.-'' áéíóú"
  cooldown-seconds: 10

gender:
  values: [Male, Female, Other]
  default: Unset
  cooldown-seconds: 10

description:
  length-minimum: 3
  cooldown-seconds: 10
  default-template: "A{n} {race} in {continent}."

profile-view:
  require-sneak: true
  require-empty-hand: true
  cooldown-seconds: 0

profile-format:
  - "&e{display_no_mask}'s &7Character Description:"
  - "&fName: &e{display_no_mask}"
  - "&fGender: &e{gender}"
  - "&fAge: &e{age}"
  - "&fRace: &e{race}"
  - ""
  - "&fDescription:"
  - "&7{description}"
```

Profile is blocked when target is masked.

### 10.2 `chat.yml`

```yaml
bypass-cooldown-perm: rpchar.chat.bypasscooldown
default: rp

channels:
  rp:
    commands: [rp]
    use-perm: rpchar.chat.use
    read-perm: rpchar.chat.use
    color-code-perm: rpchar.chat.colors
    format: '&f{display}&e: &f"&e{message}&f"'
    range: 15
    cooldown: 0

  shout:
    commands: [shout]
    use-perm: rpchar.chat.use
    read-perm: rpchar.chat.use
    color-code-perm: rpchar.chat.colors
    format: '&f{display}&6 shouts: &f"&6{message}&f"'
    range: 24
    cooldown: 0

  yell:
    commands: [yell, y]
    use-perm: rpchar.chat.use
    read-perm: rpchar.chat.use
    color-code-perm: rpchar.chat.colors
    format: '&f{display}&c yells: &f"&c&l{message}&f"'
    range: 48
    cooldown: 0

  whisper:
    commands: [whisper, wh]
    use-perm: rpchar.chat.use
    read-perm: rpchar.chat.use
    color-code-perm: rpchar.chat.colors
    format: '&f{display}#55aaff whispers: &f&o"#55aaff&o{message}&f&o"'
    range: 2
    cooldown: 0

  looc:
    commands: [looc]
    use-perm: rpchar.chat.use
    read-perm: rpchar.chat.use
    color-code-perm: rpchar.chat.colors
    format: '&f[&2LOOC&f] &f{player}: &7{message}'
    range: 20
    cooldown: 0

  ooc:
    commands: [ooc]
    use-perm: rpchar.chat.use
    read-perm: rpchar.chat.use
    color-code-perm: rpchar.chat.colors
    format: '&f[&9OOC&f] &f{player}: &7{message}'

  action:
    commands: [me]
    use-perm: rpchar.chat.use
    read-perm: rpchar.chat.use
    color-code-perm: rpchar.chat.colors
    format: '&e* &f{display} &d{message}'
    range: 20
    cooldown: 0

  admin:
    commands: [admin, a]
    use-perm: rpchar.chat.admin
    read-perm: rpchar.chat.admin
    color-code-perm: rpchar.chat.colors
    format: '&f[&cAdmin&f] &e{player}: &f{message}'

  helper:
    commands: [helper, h]
    use-perm: rpchar.chat.helper
    read-perm: rpchar.chat.helper
    color-code-perm: rpchar.chat.colors
    format: '&f[&dHelper&f] &e{player}: &f{message}'

  dm:
    commands: [dm, narrate]
    use-perm: rpchar.chat.admin
    read-perm: rpchar.chat.admin
    color-code-perm: rpchar.chat.colors
    format: '&7{message}'
    range: 64
    cooldown: 0
```

### 10.3 `masks.yml`

```yaml
masked-label: "Masked"

masks:
  ghost_mask:
    item: m.masks.ghost_mask
```

### 10.4 `rolls.yml`

```yaml
use-perm: rpchar.roll
command-aliases: [roll, tfmc roll]

default:
  min: 1
  max: 100

d20:
  min: 1
  max: 20

broadcast:
  text: '&e{player} &7rolled a &6{roll}{modifier} &7out of {max}.'
  range: 20

# MMOCore attribute value 0-20 → modifier -5..+5
attribute-modifiers:
  strength:     { 0: -5, 1: -5, 2: -4, 3: -4, 4: -3, 5: -3, 6: -2, 7: -2, 8: -1, 9: -1, 10: 0, 11: 0, 12: 1, 13: 1, 14: 2, 15: 2, 16: 3, 17: 3, 18: 4, 19: 4, 20: 5 }
  dexterity:    { ... }
  constitution: { ... }
  intelligence: { ... }
  wisdom:       { ... }
  charisma:     { ... }
```

---

## 11. Permissions (`rpchar.*`)

Greenfield permission nodes — assign via LuckPerms on the new season server.

### Persona / profile

| Permission | Grants |
|------------|--------|
| `rpchar.persona.set` | `/char alias`, gender, description |
| `rpchar.namecolour` | `/char namecolour` |
| `rpchar.persona.colors` | colour codes in description text (if supported) |
| `rpchar.profile` | shift-right-click profile |
| `rpchar.persona.override` | admin override another player's persona |
| `rpchar.persona.check` | view persona field values |
| `rpchar.persona.bypasscooldown` | skip persona field cooldowns |

### Chat

| Permission | Grants |
|------------|--------|
| `rpchar.chat.use` | use/read RP channels |
| `rpchar.chat.colors` | colour codes in chat messages |
| `rpchar.chat.bypasscooldown` | skip channel cooldowns |
| `rpchar.chat.admin` | admin + dm channels |
| `rpchar.chat.helper` | helper channel |

### Rolls

| Permission | Grants |
|------------|--------|
| `rpchar.roll` | `/roll`, `/tfmc roll` |
| `rpchar.roll.alt` | extended roll range (if configured) |

---

## 12. Deploy checklist

New season — no data migration. Deploy when Phases 1–7 are complete on staging.

### Staging

1. [ ] Complete Phases 1–7; run feature checklist (section 13)
2. [ ] Deploy `profile.yml`, `chat.yml`, `masks.yml`, `rolls.yml`
3. [ ] Configure TAB: `%rpcharacters_display_no_mask%`
4. [ ] Assign `rpchar.*` permissions in LuckPerms
5. [ ] Smoke-test: chat, mask, profile, rolls, TAB, conversations

### Production

6. [ ] Stop server
7. [ ] Deploy RPCharacters jar (all persona modules)
8. [ ] Deploy Thievery build without mask layer
9. [ ] Remove old external chat/persona/roll plugins if still present
10. [ ] Remove ConditionalEvents roll events (replaced by native rolls)
11. [ ] Start server; repeat smoke-test

---

## 13. Feature checklist

Must pass on staging before production deploy.

### Chat

- [ ] Channels: `rp`, `shout`, `yell`, `whisper`, `looc`, `ooc`, `action`, `admin`, `helper`, `dm`
- [ ] `default: rp` for plain chat (no command)
- [ ] Colour permission gating on message body (`rpchar.chat.colors`)
- [ ] Channel cooldowns
- [ ] **Deferred:** action-channel `*` emphasis parsing in `/me`
- [ ] **Deferred:** channel toggle/switcher (`/chtsw`) — port in Phase 9 if needed

### Persona / profile

- [ ] Shift-crouch right-click, empty hand, profile format
- [ ] Alias length 3–24, charset validation
- [ ] `/char alias` strips colour; colour only via `/char namecolour`
- [ ] Gender list: Male, Female, Other
- [ ] Default description template with continent

### Rolls

- [ ] `/roll` default 1–100, range 20
- [ ] `/tfmc roll <d20> [±N]` manual modifier
- [ ] `/tfmc roll <attribute>` MMOCore modifier table

### Placeholders

- [ ] `%rpcharacters_display_no_mask%` on TAB
- [ ] `%rpcharacters_display%` in chat `{display}` token
- [ ] `%rpcharacters_name%` for creation identity where needed

### Masks

- [ ] Chat: masked → plain `Masked`, no colour on label
- [ ] Profile blocked when target masked
- [ ] TAB unchanged when masked (`%rpcharacters_display_no_mask%` stable)
- [ ] LOOC/OOC use `{player}` — mask does not affect those channels

### Conversations (existing)

- [ ] `ConversationManager` listens to `CharacterChatEvent` only
- [ ] Plain chat counts as `rp` channel
- [ ] LOOC excluded; masked speakers skipped

---

## 14. Risk register

| Risk | Impact | Mitigation |
|------|--------|------------|
| TAB uses wrong placeholder | Wrong tab names | Document `%rpcharacters_display_no_mask%` in deploy checklist |
| Channel toggle/switcher users | Lost `/chtsw` | Defer to Phase 9 or port early |
| Action `*` parsing in `/me` | Missing emphasis styling | Low priority |
| Thievery mask + RPC mask both active | Double chat | Deploy together; brief maintenance window |
| Dual chat plugins during staging | Double messages | Only run RPCharacters chat on staging |

---

## 15. Package structure

```
net.tfminecraft.RPCharacters
├── identity/          DisplayIdentityService, NameColour, MaskService
├── placeholder/       RpCharactersExpansion
├── chat/              ChatManager, Channel, CharacterChatEvent
├── profile/           ProfileManager, CharacterProfileViewEvent
├── roll/              RollManager
├── calendar/          Phase 7
└── command/           CharCommand (persona); CommandManager unchanged
```

---

## 16. Dependencies (Phase 2+)

| Dependency | Scope | Notes |
|------------|-------|-------|
| PlaceholderAPI 2.11.x | `provided` / `softdepend` | systemPath to Reference Libs |
| TLibs | existing `depend` | item path check for masks, hex formatting |
| MMOCore | existing `depend` | attribute rolls |

**Explicitly not adding:** TAB plugin API, `Player#setPlayerListName`, any tab list mutation.

---

## 17. Implementation phases

| Phase | Delivers |
|-------|----------|
| **0** | This document |
| **1** | Data model + persistence; remove external persona dispatch |
| **2** | PlaceholderAPI + `/char` commands |
| **3** | `MaskService` + `masks.yml`; remove `MaskChecker` / Thievery softdepend |
| **4** | `ChatManager` + `CharacterChatEvent`; rewrite `ConversationManager` |
| **5** | `ProfileManager` + `CharacterProfileViewEvent` |
| **6** | `RollManager`; remove ConditionalEvents rolls |
| **7** | Calendar, `birthday`, dynamic `%rpcharacters_age%` |
| **8** | Deploy: remove Thievery mask layer + old external plugins |

---

## 18. Sign-off

| Role | Name | Date | Approved |
|------|------|------|----------|
| Server owner | | | [ ] |
| Plugin dev | | | [ ] |

**Proceed to Phase 1 when:** sign-off checked and deferred items (channel toggle/switcher, action `*` parsing) are accepted.
