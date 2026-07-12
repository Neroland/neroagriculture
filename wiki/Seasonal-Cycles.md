# Seasonal and Stellar Cycles

Cycles are per-dimension seasons (or stellar states) that gently raise or lower how fast crops grow and how
much they yield. They are entirely data-driven, deterministic, and bounded, so they add rhythm without ever
becoming an exploit.

## How cycles work

Each cycle is a list of **phases** that repeat over a fixed **period** of ticks. The active phase is a pure
function of world time, so restarts, `/time` changes, sleeping, and unloaded chunks all resolve to the same
phase — you can never jump time to duplicate a harvest. Each phase supplies bounded **growth**, **yield**,
and **environment** multipliers (clamped to 0.1–4.0), which multiply the normal growth chance and harvest
amount. With cycles disabled (`cycles.enabled = false`) or no profile for a dimension, everything resolves
to 1.0.

A gentle four-season cycle ships for the Overworld by default. Active and upcoming phases show in the crop
shift-click readout and in the greenhouse and crop-tower status.

## Datapack configuration

Add or replace a dimension's cycle at
`data/neroagriculture/neroagriculture/cycles/<dimension>.json` (the file name is the dimension path):

```json
{
  "period": 48000,
  "phase_offset": 0,
  "phases": [
    { "display_key": "cycle.neroagriculture.stellar_bright", "growth": 1.4, "yield": 1.2 },
    { "display_key": "cycle.neroagriculture.stellar_dark", "growth": 0.6, "yield": 0.9 }
  ]
}
```

`growth`, `yield` and `environment` default to `1.0` if omitted and are always clamped to the safe range.
Performance is never a concern: the active modifier is cached on coarse time buckets and computed with a
couple of arithmetic operations — cycles never evaluate a calendar or scan crops per tick.

## Other mods

Space weather and events can layer on through the `CycleApi` provider seam — Nerospace or NeroEvents may
register a provider whose modifier multiplies the profile's. When no provider is registered the external
contribution is exactly 1.0, so cycles work fully standalone.

See also: [Growth Conditions](Growth-Conditions.md), [Resource Crops](Resource-Crops.md).
