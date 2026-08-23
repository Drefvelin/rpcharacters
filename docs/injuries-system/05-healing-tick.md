# Batch 05 — Healing tick

**Depends on:** [02](./02-trait-state-persistence.md), [03](./03-trait-runtime-effects.md)  
**Blocks:** 12

## Goal

Decrement healing duration while player is online with the injury on the active character.

## `InjuryHealingService` (or extend `PlayerManager`)

- Register repeating task (e.g. every 20 ticks or 1 minute wall clock)
- For each online player with active character:
  - For each owned trait with `duration` in YAML:
    - Subtract elapsed ms from `duration-remaining-ms`
    - If `<= 0`: remove trait, lost message, update integrator, clear state
    - Else: `character.update()` if progress crossed int threshold (optional optimization: only on minute boundaries)

## Tick rules

- **Only** when character is active (`character.isActive()`)
- **Only** while player online
- Inactive characters or offline: duration frozen

## Config (optional `injuries.yml` or `config.yml`)

```yaml
healing-tick-interval: 1m
```

Default 1 minute if omitted.

## Acceptance

- [x] Active character heals over time; inactive does not
- [x] Fully healed injury removed with lost message
- [x] Attribute penalties decrease as duration decreases

## Implemented

- `healing-tick-interval: 1m` in `injuries.yml`, read by `InjuryPoolLoader`
- `InjuryHealingService` repeating task: decrements `duration-remaining-ms`, removes healed traits with lost message, refreshes integrator on active characters
- Started from `PlayerManager.start()`
