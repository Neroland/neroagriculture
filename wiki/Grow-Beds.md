# Grow Beds

Grow beds determine the highest material tier a resource crop may cultivate. A higher-tier bed may host a
lower-tier material, but a lower-tier bed never satisfies a higher-tier crop. Use any bed to open its screen.
When holding a seed, use the top to plant by hand or use a side to open the screen.

| Bed | Supported tier | Growth resources |
| --- | --- | --- |
| Territe | Territe | Passive |
| Forgite | Forgite and below | NF plus nutrient solution |
| Orbite | Orbite and below | NF plus nutrient solution |
| Colonite | Colonite and below | NF plus nutrient solution |
| Voidite | Voidite and below | NF plus nutrient solution |

Forgite through Voidite beds use Neroland Core's energy and fluid storage contracts. Loader-specific
capability seams expose input on every side. Stored NF and nutrient solution persist with the block entity
and synchronize when their contents change. Each bed checks its own crop once per second for planting
and harvesting; growth atomically consumes the configured costs only when a random growth step succeeds.

A grow bed records its placer (opt-out — see [Privacy](Privacy-and-Erasure.md)) so the crop above it
checks progression gates against the bed's owner rather than whoever stands nearest. Removing a bed by
any means — survival or creative break, or an explosion — drops its stored seed and harvest output, so
nothing in it is ever voided.

## The bed screen

Open any bed to see:

- **Energy** and **Nutrient** gauges, the bed's tier, and its seed and harvest-output slots.
- A **Growth gauge** beside the nutrient bar showing how far the crop planted above the bed has grown —
  the bar fills with the crop's age and the label reads the percentage (e.g. "Growth 71%"). It works for
  resource and species crops alike, and reads "Growth —" while the bed is bare.
- A **status line** naming the live reason the crop above the bed is not advancing — not enough light,
  wrong dimension, hostile environment, needs a greenhouse, gate closed, out of NF or nutrient, and so on.
  It reads the same rules the growth tick uses. A bare bed reads "No crop planted" unless its input
  holds a seed with an unknown or disabled material or one blocked by bed tier, progression or dimension.
- A scrollable **seed compatibility panel** down the right-hand column listing every material in the
  server's catalog, split into what this bed grows now and what needs a better bed. Each row shows the
  material's colour and its tier tag, so the panel doubles as the upgrade ladder. Scroll it with the
  mouse wheel. On a fresh join the panel stays empty until the server has sent its material catalog.

Territe beds show a passive label instead of energy and nutrient gauges. They can automatically plant
one resource seed from the input slot and collect harvests in the output slots without power; crops still
need their usual light, climate and other growing conditions. Existing Territe beds can be opened after updating.
