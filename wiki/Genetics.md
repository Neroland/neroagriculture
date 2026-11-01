# Genetics

Every seed can carry **genetics** — five traits that tune how a crop performs. Genetics travel with the
seed and the planted crop through planting, harvest, breaking, the Planter/Harvester, storage, and reloads.
Seed tooltips show each current trait and the total against the 15-point cap.

A resource crop can roll the configured bonus-seed chance only on its first harvest. The bonus seed
inherits the parent's genetics and begins its own harvest history; the planted parent keeps its history
when broken and replanted. Each seed therefore gets at most one reproduction opportunity instead of
repeatedly cloning improved genetics on every harvest. Area harvesters and crop towers do not award
bonus seeds. Existing seeds and their traits are retained.

## Traits and caps

| Trait | Effect |
| --- | --- |
| Yield | Extra output per harvest |
| Speed | Faster growth (larger age steps) |
| Hardiness | Tolerate an otherwise hostile open-air world, up to a configured limit |
| Oxygen output | Offsets a greenhouse's nutrient upkeep |
| Food potency | Raises a food's signature effect, never above the species' own cap |

Each trait ranges **0–5**, and the **sum of all traits is capped at 15**. These caps are enforced the
instant genetics are decoded, networked, spliced, mutated, or read from a component — a hand-edited or
forged value is always clamped back into range, so no trait can exceed 5 and no seed can exceed a total of
15. Hardiness relaxes only open-air hostility; it can never skip the engineered greenhouse requirement that
high-tier crops have.

## Genetics Station

The NF-powered **Genetics Station** works on seeds:

- **Analyse** — right-click the station with a seed in hand to print its traits and total.
- **Splice** — two seeds in the input slots produce one output seed whose every trait is the higher of the
  two parents, then clamped to the total cap.
- **Upgrade** — a seed plus Resource Fragment raises the seed's lowest trait by one.

All of these use the same deterministic, capped maths, so an operation can never push a seed past its caps.

See also: [Breeding](Breeding.md), [Pollination](Pollination.md).
