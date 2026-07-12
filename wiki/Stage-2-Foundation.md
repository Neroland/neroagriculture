# Stage 2 foundation and stable IDs

Stage 2 fixes the save-facing registry catalog before gameplay systems start. Material-specific
resource seeds and essences use the versioned `material_variant` data component, so a datapack or
another Nero mod can introduce a material without creating another registered item.

## Core content

- Generic `resource_seed` and `material_essence`, five neutral essences, and blank/charged seeds.
- Nutrient solution as a still/flowing fluid, bucket, and portable canister.
- Five affinity grow beds and the generic resource crop.
- Essence Extractor, Essence Infuser, Seed Synthesizer, Planter, and Harvester.
- Stable shells for food and alien crops, research, greenhouses, fertiliser, genetics, oxygen,
  biofuel, crop towers, pollination, compacted essence/decor, and terraforming.

Machine shells persist four item slots, a Core fluid buffer, Core energy, upgrades, and universal
side configuration. Fabric, Forge, and NeoForge expose those stores through their loader APIs and
Core's cross-mod energy/fluid surfaces.

## Integration contract

`AgricultureApi` reserves provider/event seams for diet outputs, cultivation and research
objectives, premium goods, cultivation modifiers (including pollination and seasons), drone
assistance, biofuel consumers, and terraforming events. The one Stage 2 serverbound packet uses a
fixed-size payload and validates action bounds, player proximity, loaded position, open menu, and
target block entity before acting.

Baseline models deliberately reuse vanilla textures. Replacing them later changes presentation,
not registry ids or worlds.
