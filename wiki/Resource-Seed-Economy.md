# Resource-seed economy

NeroAgriculture reframes farming around resources: you **craft a resource seed, grow it, and harvest
the resource**. Seeds are the valuable, tiered asset — and they work for any resource discovered
through common tags, vanilla or modded. The whole progression is **standalone**: only Neroland Core is
required.

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

Because tier-1 extraction is ungated, the entire ladder is reachable from a fresh start with Core only.
(A loaded sibling mod may *additionally* gate higher tiers — see
[Sibling-mod progression overlays](Sibling-Overlays.md).)

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
Higher tiers need higher-tier fragments, stronger beds, more NeroFlux, and an unlocked gate.

## Automation

The full loop automates end to end once you have paid in: hoppers/pipes, the
[Planter & Harvester](Automation.md), and [crop towers](Crop-Towers.md).
