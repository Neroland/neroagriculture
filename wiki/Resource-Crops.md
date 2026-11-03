# Resource Crops

One generic resource-crop block represents every catalog material. Its block state stores only the
visible age from 0 through 7; a non-ticking block entity stores the material id, resolved tier, format
version, and completed-harvest count.

Use a component-backed Resource Seed on top of a suitable grow bed to plant it. The server resolves the
material from its current catalog and checks its tier, progression gate, and dimension restriction before
placing the crop. Client-supplied tier data is never trusted.
If a datapack changes a crop's required world, an existing crop is kept intact and can only be
harvested in its allowed dimension, including by grow beds and harvesters.

A mature crop can be harvested by using it. Harvesting resets its age, increments its persistent harvest
history, and gives server-created Resource Fragment while leaving the crop planted. Breaking the crop as a
non-creative player returns exactly one Resource Seed with the allowed material and harvest history.
Pistons cannot move crops, and unsupported, exploded, or indirectly replaced crops do not create extra
seeds or fragment.

Sneak-use a planted crop for a readout of its material, tier, growth, harvest history, current and next
yield, seasonal cycle and the first condition blocking growth.

With default settings and a neutral cycle, each resource-crop random tick advances two age stages.
Fertiliser and speed genetics can improve this further.
Growth is still random and requires a loaded, ticking chunk; the setting `growth.multiplier` and the
current cycle can change the chance of growth, up to 100%.
