# Performance

NeroAgriculture is designed to stay cheap even at colony scale. The core rule is that **nothing scans the
whole world, a whole structure, or a whole work area every tick.**

## How work is bounded

- **Crops** grow on vanilla random ticks and store their identity/genetics in a non-ticking block entity —
  there is no crop registry and no world scan.
- **Area machines** (Planter, Harvester, Fertiliser Applicator) process a capped number of columns per
  pass from a rolling cursor, on an interval, phase-offset so neighbours do not all fire on the same tick,
  and skip unloaded columns.
- **Greenhouses** flood-fill their interior only on a slow safety interval or a structural change, cache the
  result, and publish it to an O(1) lookup index that crops query.
- **Crop towers** farm virtual slots (not thousands of blocks), a bounded number per pass, and cache their
  formed structure.
- **Seasonal cycles** cache the active modifier on coarse time buckets and compute the phase with a couple
  of arithmetic operations.
- **Terraforming** advances by a config-capped amount per tick and mutates no blocks at all.

## Tuning for big servers

If you run very large farms, the pacing keys let you trade throughput for tick budget:
`automation.columns_per_pass`, `automation.interval_ticks`, `crop_tower.slots_per_pass`,
`greenhouse.revalidate_ticks`, and `terraforming.progress_per_tick`. Lower per-pass counts and longer
intervals reduce per-tick cost.

## Rendering

Server logic never depends on renderer state, so client-side visual quality can be reduced without affecting
farms. (Rich crop-layer renderers and animation-quality controls are a client-focused follow-up.)

See also: [Automation](Automation.md), [Crop Towers](Crop-Towers.md), [Configuration](Configuration.md).
