# Configuration

All server-authoritative tuning lives in NeroAgriculture's config (registered through Core's config system).
Values are grouped by system; the most useful keys are listed below. Lists use comma-separated identifiers.

## Discovery and catalog

- `discovery.scan_cap` — maximum enabled catalog entries exposed/synced.
- `discovery.material_blacklist` — material ids disabled before exposure.
- `discovery.material_overrides` — per-material tier/gate/yield/conversion/enabled overrides.
- `discovery.default_tier` — tier for auto-discovered materials no heuristic recognises (default `orbite`;
  invalid values fall back to `orbite`).

## Growth, beds and yield

- `growth.multiplier`, `growth.yield_multiplier` — global growth speed and yield scaling.
- `growth.tier_yield_cap_base`, `growth.tier_yield_cap_step` — the hard per-tier yield ceiling.
- `growth.controlled_tier` — lowest tier that always needs a sealed greenhouse.
- `grow_beds.energy_per_growth`, `grow_beds.nutrient_per_growth_mb` — powered-bed running costs.

## Progression

- `progression.require_research` — require Seed Research Bench research before synthesis/conversion
  (**on by default**; hard tier gates remain off).
- `progression.sibling_overlays` — cross-mod arc-gate overlays: `off` (default), `auto`, or `on`
  (see [Sibling Overlays](Sibling-Overlays.md)).

## Automation, fertiliser and privacy

- `automation.interval_ticks`, `automation.columns_per_pass`, `automation.energy_per_op` — Planter/Harvester pacing.
- `automation.track_owner` — record placer UUIDs for claim checks and owner-bound progression (opt-out;
  see [Privacy](Privacy-and-Erasure.md)).
- `privacy.erasure_retention_days` — how long erasure tombstones are kept so owners saved in unloaded
  chunks are still purged when those chunks load (default 90 days).
- `fertiliser.max_dose`, `fertiliser.duration_ticks` — fertiliser cap and lifetime.

## Greenhouse, oxygen and bioreactor

- `greenhouse.volume_cap`, `greenhouse.revalidate_ticks`, `greenhouse.upkeep_ticks`.
- `greenhouse.nf_per_32_volume`, `greenhouse.nutrient_mb_per_crop`, `greenhouse.oxygen_cap_per_32_volume`.
- `bioreactor.*` — biomass → nutrient rates and loss.

## Genetics, cycles, biofuel, towers, terraforming

- `genetics.hardiness_hostile_relax`, `genetics.pollination_chance_percent`, `genetics.mutation_chance_percent`,
  `genetics.beacon_range`.
- `cycles.enabled` — seasonal/stellar modifiers on/off.
- `biofuel.energy_nf_per_mb`, `biofuel.primary_generation_ceiling_nf_per_mb`.
- `crop_tower.min_height`, `crop_tower.max_height`, `crop_tower.slots_per_layer`, `crop_tower.slots_per_pass`.
- `crop_tower.hostile_environment_nf_multiplier` — NF surcharge for tower layers whose environment would
  fail the crop's climate check (default 4; towers keep growing, they just pay more).
- `terraforming.enabled`, `terraforming.region_radius`, `terraforming.total_progress`,
  `terraforming.progress_per_tick`.

See also: [Administration](Administration.md), [Material Recipe Datapacks](Material-Recipe-Datapacks.md).
