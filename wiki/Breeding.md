# Breeding and Mutations

Beyond tuning traits, crops can produce **new strains**. Breeding is data-driven: a datapack breeding entry
names two parent species and the child species they can yield.

## The breeding tree

Breeding entries live in `data/neroagriculture/neroagriculture/breeding/<name>.json`:

```json
{
  "parent_a": "neroagriculture:food/earth_grain_loaf",
  "parent_b": "neroagriculture:food/earth_sunfruit",
  "child": "neroagriculture:food/greenxertz_driftmelon",
  "research": "nerolandcore:some_gate"
}
```

The optional `research` field ties a breed to the [Research Bench](Seed-Research.md) discovery tree — until
that milestone is met, the pair will not produce its child. Built-in entries ship with the mod and can be
added to or replaced by datapacks.

Breeding never bypasses progression. A bred child seed still has to satisfy its own tier, gate, environment
and research rules to grow, and **natural alien strains can never be a breeding child** — those are found in
the world, not created.

## Mutations

When a cross succeeds it produces a child seed whose genetics are the **spliced** traits of both parents.
With a configurable chance the child is also **mutated** — one trait raised a further step — using
deterministic, server-authoritative, seeded maths bounded by the same per-trait and total caps. Mutation
can never take a seed past its caps.

See also: [Genetics](Genetics.md), [Pollination](Pollination.md).
