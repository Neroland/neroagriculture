# Resource Crops

One generic resource-crop block represents every catalog material. Its block state stores only the
visible age from 0 through 7; a non-ticking block entity stores the material id, resolved tier, format
version, and completed-harvest count.

Use a component-backed Resource Seed on top of a suitable grow bed to plant it. The server resolves the
material from its current catalog and checks its tier, progression gate, and dimension restriction before
placing the crop. Client-supplied tier data is never trusted.

A mature crop can be harvested by using it. Harvesting resets its age, increments its persistent harvest
history, and gives server-created Material Essence while leaving the crop planted. Breaking the crop as a
non-creative player returns exactly one Resource Seed with the allowed material and harvest history.
Pistons cannot move crops, and unsupported, exploded, or indirectly replaced crops do not create extra
seeds or essence.

Sneak-use a planted crop for a diagnostic line containing material, tier, age, harvest history, current
yield, and the first condition blocking growth.
