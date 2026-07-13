# Grow Beds

Grow beds determine the highest material tier a resource crop may cultivate. A higher-tier bed may host a
lower-tier material, but a lower-tier bed never satisfies a higher-tier crop.

| Bed | Supported tier | Growth resources |
| --- | --- | --- |
| Territe | Territe | Passive |
| Forgite | Forgite and below | NF plus nutrient solution |
| Orbite | Orbite and below | NF plus nutrient solution |
| Colonite | Colonite and below | NF plus nutrient solution |
| Voidite | Voidite and below | NF plus nutrient solution |

Forgite through Voidite beds use Neroland Core's energy and fluid storage contracts. Loader-specific
capability seams expose input on every side. Stored NF and nutrient solution persist with the block entity
and synchronize when their contents change. Beds do not tick or scan the world; the crop atomically checks
and consumes the configured costs only when a random growth step succeeds.
