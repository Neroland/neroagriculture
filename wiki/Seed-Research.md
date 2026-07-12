# Seed Research

The Seed Research Bench turns physical samples into Core-managed knowledge. Insert a sample, open the
bench, and select **Research**. The server chooses the first matching `seed_researching` recipe by recipe
id and consumes the configured sample count only after every milestone write succeeds.

Resource research first requires the typed Core `material_discovered` milestone. A sample from the owning
mod can satisfy owner-mod evidence; an unknown third-party sample handled by the server can satisfy pickup
evidence. Planet-bound materials remain locked until Nerospace records the required historical visit.
Agriculture never infers a planet visit from merely possessing an item.

Research outcomes are separated into resource, food, and alien branches through these typed milestones:

- `neroagriculture:resource_seed_researched`
- `neroagriculture:food_seed_researched`
- `neroagriculture:alien_seed_researched`

The milestone definitions are datapack-driven and player-scoped. Persistence, client sync, subject export,
and erasure are supplied by Neroland Core's `MaterialMilestones` and `PlayerDataErasure` systems. The bench
stores no player identity, timestamps, or research history of its own.
