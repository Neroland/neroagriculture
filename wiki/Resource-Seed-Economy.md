# Resource-seed economy

NeroAgriculture reframes farming around resources: you **craft a resource seed, grow it, and harvest
the resource**. Seeds are the valuable, tiered asset — and they work for any resource discovered
through common tags, vanilla or modded. Discovery alone does not provide the necessary machine recipes
for every catalog entry. The craftable Field Generator provides starter power from coal or charcoal.

## Fragments

There are two kinds of fragment:

- **Tier Fragments** — the five-step ladder currency, in Cosmic-ascent order:
  **Territe → Forgite → Orbite → Colonite → Voidite**.
- **Resource Fragments** — the per-resource harvest drop (e.g. an *Iron* Resource Fragment), tinted to
  the resource's ingot colour, which you convert into the resource itself.

## The ladder and native gates

Tier 1 (Territe) is open from the start. Upgrading fragments up the ladder in the **Fragment Infuser**
(4× tier N → 1× tier N+1, costing NeroFlux and time that scale steeply by tier) **opens the next
native progression gate**:

| Producing | Opens gate | Unlocks tier |
| --------- | ---------- | ------------ |
| Territe | `neroagriculture:refinement` | Forgite (T2) |
| Forgite | `neroagriculture:synthesis` | Orbite (T3) |
| Orbite | `neroagriculture:transmutation` | Colonite (T4) |
| Colonite | `neroagriculture:ascension` | Voidite (T5) |

Tier-1 extraction is ungated and requires power. A loaded sibling mod may *additionally* gate
higher tiers — see
[Sibling-mod progression overlays](Sibling-Overlays.md).

## Prospora Seed — the base

The **Prospora Seed** is the crafting core of every resource seed. Craft it from **Territe Fragments +
wheat seeds**. Then synthesize a resource seed in the **Seed Synthesizer**:

```text
Resource Seed(X) = the real resource X + N matching Tier Fragments + one Prospora Seed
```

The Prospora Seed also plants a **base crop** on farmland: it grows like wheat and, on harvest,
drops **Territe Fragments** plus a Prospora Seed — a renewable, standalone way into the ladder.

Requiring the real resource keeps seeds an **amplifier** — you can only farm a resource you already
possess — so a resource from an absent mod simply has no craftable seed.

## Grow and convert

Plant the resource seed on a [grow bed](Grow-Beds.md) of at least the resource's tier, grow it, and
harvest **Resource Fragments**. Convert **N Fragments → the resource** (N is per-tier, config-tunable).
Higher tiers need higher-tier fragments, stronger beds and more NeroFlux. Explicit material gates and
optional sibling overlays can add further requirements.

## Default balance reference

The following are baseline built-in values before genetics, fertiliser, cycle modifiers or pack
overrides. A seed requires one material sample, one Prospora Seed and the shown number of matching
neutral tier fragments; the research bench consumes a separate sample. Higher-tier neutral fragments
can be condensed 4:1 from the preceding tier. The table shows resource fragments per harvest and the
number needed for one resource, not a guarantee of a particular real-time rate.

| Tier | Example | Tier fragments per seed | Fresh → mature yield | Harvests to mature | Fragments per resource |
| --- | --- | ---: | ---: | ---: | ---: |
| Territe | Coal | 2 | 2 → 3 | 32 | 8 |
| Forgite | Iron | 3 | 1 → 4 | 64 | 12 |
| Orbite | Diamond | 4 | 1 → 5 | 96 | 16 |
| Colonite | Nether Star | 5 | 1 → 6 | 128 | 20 |
| Voidite | Echo Shard | 6 | 1 → 7 | 160 | 24 |

Every row assumes that the resource has been found and researched, and that its matching bed or a
higher one is available. Territe is passive; Forgite needs a powered, nutrient-fed bed. Orbite,
Colonite and Voidite also need a sealed, powered greenhouse by default. Hostile worlds can require a
greenhouse at any tier, and a material may specify a particular dimension. Native hard gates and sibling
overlays are optional rather than prerequisites in the default configuration.

At the neutral cycle multiplier and default growth setting, a resource crop advances two of its seven
growth stages per random tick, reaching maturity after four eligible ticks.
Actual elapsed time depends on Minecraft's random-tick rules, chunk activity, cycles and growing
conditions. As a mathematical estimate, with a continuously ticking chunk, 20 game ticks per second,
`random_tick_speed` 3 and a neutral cycle, one crop is selected about once per 4,096/3 game ticks. That
implies roughly 4.6 minutes to mature for each tier, on average. These
are expectations, not guarantees or measured play times; fertiliser, genetics and cycles change them.
At the default cost and without growth bonuses, a powered crop's four advances consume
160 NF and 100 mB of nutrient, in addition to any greenhouse upkeep. A fresh coal crop yields two fragments per harvest, so four harvests return one coal after
conversion; fragments left over from extraction can contribute as well. These are current code values
and still need survival playtesting, not final
balance targets. Datapack definitions, including planet-specific materials, may set different yields,
ramps and conversion costs.

Coal farming can eventually supply renewable Field Generator fuel: producing coal through crop growth
costs 400 NF for conversion and that coal can generate 3,200 NF. This deliberate renewable loop also
requires the initial seed, repeated growth and a running generator; higher-tier beds and automation add
their own energy and nutrient costs.

## Automation

The full loop automates end to end once you have paid in: hoppers/pipes, the
[Planter & Harvester](Automation.md), and [crop towers](Crop-Towers.md).

The tier yield cap applies to the final fragment count after genetics, fertiliser, cycle and automation bonuses. Manual harvesting, grow-bed storage, harvesters and crop towers follow the same cap. Catalog-defined colours are applied to newly produced seeds and fragments; older items retain their saved tint and may occupy separate inventory stacks after a colour changes.
