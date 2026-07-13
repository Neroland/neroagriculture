# Biofuel and Fragment Blocks

Stage 12 turns farm surplus into power and compacts fragments into storage and decoration.

## Biofuel

**Biomass** (from crops and the closed loop) feeds the **Biofuel Converter** — an NF machine that turns
Biomass into **Biofuel** fluid, with an explicit loss and a periodic recoverable **Crop Waste** byproduct.
Item, fluid, and energy I/O all go through Core side configuration, so hoppers, pipes and fluid transport
work across every loader.

Biofuel is a **renewable baseload**: each millibucket is worth a configured amount of Nero Flux
(`biofuel.energy_nf_per_mb`), deliberately kept **below** an equivalently-gated primary generator's ceiling
(`biofuel.primary_generation_ceiling_nf_per_mb`) so a farm can supplement power without out-earning
dedicated generation. Because the conversion is one-way and lossy, the farm-to-fuel loop can never mint net
items or energy.

The converter offers surplus biofuel to power mods through the public `AgricultureApi.BIOFUEL` provider
seam — Nerotech or NeroPower register a consumer and pull fuel; no consumer internals are imported. Biofuel
and its bucket are tagged under `c:` common fluid/item tags so other mods can find them. Standalone, the
biofuel simply accumulates as a fluid you can pipe or bucket out.

## Compacted fragment and decorative blocks

Each of the five neutral fragments (Territe, Forgite, Orbite, Colonite, Voidite) compacts **9 fragment →
1 block** and back **1 block → 9 fragment**, with exact, lossless accounting — handy for compact storage.
Resource Fragment can also be crafted into a generic **Fragment Decor** block for building. The fragment blocks
and decor are tagged under `c:` common block tags for NeroDecor compatibility, with no hard dependency on it.

See also: [Resource Fragment](Resource-Fragment.md), [Oxygen and Life Support](Oxygen-and-Life-Support.md).
