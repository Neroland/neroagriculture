# Growth Conditions

Each random growth attempt is server-authoritative and local to the crop. It fails closed at the first
invalid condition:

1. The material must exist and remain enabled in the current server catalog.
2. Any material progression gate must be open for a nearby player.
3. The supporting grow bed must meet or exceed the material tier.
4. Block light must be at least 9.
5. Any catalog dimension restriction must match the current dimension.
6. Industrial through Deepvoid crops need the configured NF and nutrient amount in their powered bed.

Terran growth is passive and does not consume resources. Powered-bed resources are simulated together
before either store is mutated, preventing partial consumption when one requirement is missing. Growth uses
random block ticks and never maintains a crop registry or performs a world scan.

The server configuration controls growth speed, yield scaling, and per-step powered-bed costs. Catalog
reloads take effect on subsequent attempts; a removed or disabled material keeps its stored identity so it
can be recovered as a seed, but it cannot grow or harvest until valid again.
