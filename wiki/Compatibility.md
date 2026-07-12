# Compatibility

NeroAgriculture hard-depends on **Neroland Core alone**. Every other integration is optional and dormant
until the other mod is present — the mod runs fully standalone with nothing registered. External mods
interoperate only through Core capabilities, `c:` common tags, and the provider seams in
`CompatContracts` — no third-party classes appear anywhere in `common/`.

## Integration routes

| Mod | Route | Status |
| --- | --- | --- |
| Neroland Core | Hard dependency (registration, machines, NF energy, upgrades, side config, gates, fluids) | Live |
| Nerospace | `EnvironmentApi` (planet atmosphere), `OxygenApi`, `CycleApi` (space weather), terraforming overlay | Contract-ready; live wiring pending Nerospace 26.x |
| Nerotech / NeroPower | `AgricultureApi.BIOFUEL` consumer seam + `c:` biofuel fluid/item tags | Contract-ready; live when a consumer registers |
| Energized Power | Core Forge-Energy bridge + `c:` material/biofuel tags | Live (targets 26.1+/26.2) |
| Create | Generic item/fluid I/O via Core side config + `c:` tags (no Create classes) | Dormant until Create ports to 26.1+ |
| AE2 | Component-backed seed/essence variants via generic item handlers | Dormant until AE2 ports to 26.1+ |
| Mekanism | Ore discovery via `c:ores/*`, material/biofuel tags | Dormant until Mekanism ports to 26.1+ |
| Ad Astra | Dimension environment via `DimensionEnvironments` datapack + `EnvironmentApi` | Dormant until Ad Astra ports to 26.1+ |
| NeroLogistics | Moves component-backed items via generic `WorldlyContainer`/side config | Contract-ready |
| NeroColonies | `AgricultureApi.DietProvider` (varied-diet metadata) | Contract-ready |
| NeroEconomy | `AgricultureApi.PremiumGoodsProvider` | Contract-ready |
| NeroQuests | `AgricultureApi.ObjectiveProvider`, terraforming/pollination events | Contract-ready |
| NeroCreatures | `AgricultureApi.DroneAssistanceProvider` | Contract-ready |
| NeroEvents | `CycleApi.Provider`, terraforming events | Contract-ready |

## For integrators

`za.co.neroland.neroagriculture.compat.CompatContracts` is the single entry point: it exposes
`register*`/`remove*` helpers for every seam plus `hasBiofuelConsumer()`, `externalCycle(...)` and
`environmentOrDefault(...)`. Registering is safe at any time; with nothing registered the mod uses its local
defaults (habitable environment, identity cycle, no external power/oxygen consumers).

## Honest limitations

Live, in-game verification of the external mods above (Create, AE2, Mekanism, Ad Astra) cannot be performed
until those mods publish MC 26.1+ builds — until then their routes are exercised only by the mod's own
contract tests and the generic Core/tag mechanisms, and are accurately labelled **dormant** rather than
claimed as working. Live cross-mod runtime for the Nero siblings is consolidated into the Stage 18 matrix.

See also: [Material Catalog and Crop Storage](Material-Catalog-and-Crop-Storage.md), [Biofuel and Essence
Blocks](Biofuel-and-Essence-Blocks.md).
