# Batch 12 — Verify and deploy

**Depends on:** All batches 01–11

## Goal

Test matrix, deploy steps, operator checklist.

## Manual test matrix

### Permadeath zone

| # | Setup | Death | Expected |
|---|-------|-------|----------|
| 1 | 0 injuries | zone death | Roll permakill at 0%; else gain healing injury |
| 2 | 1 healing `broken_arm` | zone death | Roll permakill at 10%; else convert to `one_handed`, no new injury |
| 3 | 1 permanent + 0 healing | zone death | Roll at 10%; else new healing injury from pool |
| 4 | 2 injuries | zone death | Roll at 20% |
| 5 | prosthetic only, no injury | zone death | Prosthetic does not count; 0% from injuries |

### Healing

| # | Action | Expected |
|---|--------|----------|
| 6 | Active online 48h duration | Remaining decreases |
| 7 | Switch character / offline | Remaining frozen |
| 8 | Duration hits 0 | Trait removed, lost message |
| 9 | Remedy on healing | Instant remove |
| 10 | Remedy on permanent | No effect |

### Prosthetics

| # | Action | Expected |
|---|--------|----------|
| 11 | Install with blaze rod | Injury removed, tier 1 added |
| 12 | Upgrade twice | Tier 3, fuel % preserved |
| 13 | Arcane fuel empty | Depowered name/effects |
| 14 | Refuel arcane item | Powered restored |

### Creator

| # | Action | Expected |
|---|--------|----------|
| 15 | Skip both stages | Valid character |
| 16 | 2 permanent + 1 prosthetic | 0 + 1 point spent |
| 17 | Arcane from creator | Full fuel, disclaimer shown |

### Web

| # | Action | Expected |
|---|--------|----------|
| 18 | Create with prosthetic on web | Matches in game after claim |

## Deploy runbook

1. Build RPCharacters jar with all batches
2. Deploy `fuel-templates.yml`, `injury-progression.yml`, `prosthetics.yml` to server config folder
3. Replace `traits/injury-traits.yml`, add `traits/prosthetic-traits.yml`
4. Update `injuries.yml`, `items.yml` remedies, `stages.yml`
5. `/rpcharacter reload` or restart
6. Deploy ProvinceSystem frontend/backend if batch 10 changed
7. Update live server zip backup after smoke test

## Rollback

- Keep previous jar + config backup
- `trait-state` is additive; old jar ignores unknown JSON keys

## Status checklist

- [ ] Batches 01–11 complete
- [ ] Test matrix passed
- [ ] STAGING operator sign off
