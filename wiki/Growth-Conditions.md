# Growth Conditions

Each random growth attempt is server-authoritative and local to the crop. It fails closed at the first
invalid condition:

1. The material must exist and remain enabled in the current server catalog.
2. Any material progression gate must be open for a nearby player.
3. The supporting grow bed must meet or exceed the material tier.
4. Block light must be at least 9.
5. Any catalog dimension restriction must match the current dimension.
6. The **environment** must suit the crop (see below).
7. Forgite through Voidite crops need the configured NF and nutrient amount in their powered bed.

Territe growth is passive and does not consume resources. Powered-bed resources are simulated together
before either store is mutated, preventing partial consumption when one requirement is missing. Growth uses
random block ticks and never maintains a crop registry or performs a world scan.

## Environment and sealing

Every dimension has an environment profile — temperature plus whether the air is oxygenated and
pressurised. The Overworld (and any unclassified world) is habitable; the Nether is hot and unbreathable
and the End is unpressurised, so both are hostile open air. Datapacks under
`data/neroagriculture/neroagriculture/environments/<dimension>.json` reclassify any dimension, and when
Nerospace is installed its environment API supplies planet conditions instead.

A crop grows in the open only when the world is habitable **and** its tier is below the configured
controlled-environment threshold (Orbite by default). Higher-tier crops always need an engineered
atmosphere, and any crop in a hostile world needs one too. A formed, powered
[greenhouse](Greenhouse-Construction.md) supplies that controlled interior, so crops that fail in hostile
open air grow normally once sealed. The blocked reason a crop reports is `HOSTILE_ENVIRONMENT` (world
unsuitable, no seal) or `NEEDS_GREENHOUSE` (tier requires a seal). See
[Greenhouse Troubleshooting](Greenhouse-Troubleshooting.md) if a sealed crop still will not grow.

The server configuration controls growth speed, yield scaling, and per-step powered-bed costs. Catalog
reloads take effect on subsequent attempts; a removed or disabled material keeps its stored identity so it
can be recovered as a seed, but it cannot grow or harvest until valid again.
