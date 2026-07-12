# Pollination

Cross-[breeding](Breeding.md) happens in the field through **pollination** — no machine required.

## Passive adjacency

When two **mature** crops of a matching [breeding](Breeding.md) pair sit next to each other, each has a
small, rate-limited chance to pollinate and drop a child seed. The roll is server-authoritative and
deterministic (seeded from position and world time), and the child seed carries the spliced — and possibly
mutated — genetics of both parents. Because it rides on the crops' own random ticks, there is no per-tick
farm scan and no growing entity count.

## Pollination Beacon

The **Pollination Beacon** is an optional, NF-powered booster. Over a bounded region around it (configurable
range, worked a few columns per pass on an interval) it drives the same pollination as passive adjacency,
just at a higher rate. It is never required, spawns no insects or drones, and simply speeds up crossing on a
dense mixed farm. Right-click it for its status.

## Bounds

All pollination rolls, mutation rolls and child genetics use the deterministic, capped genetics maths, so
nothing pollination produces can exceed the [trait caps](Genetics.md). Optional insect/drone visuals, if
added later, remain presentation only — pollination itself never depends on entities.

See also: [Genetics](Genetics.md), [Breeding](Breeding.md).
