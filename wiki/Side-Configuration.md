# Side Configuration

Every NeroAgriculture machine exposes its inputs and outputs through Neroland Core's shared side-config
system, so automation from hoppers, pipes and other mods works the same way across the whole ecosystem.

## Channels

Machines publish channels for the resources they use:

- **Item** — split into input and output slot groups. The Planter accepts seeds as input; the Harvester
  offers produce as output; the Fertiliser Processor takes Biomass/Crop Waste in and gives Fertiliser out.
- **Energy (NF)** — machines accept Nero Flux but never push it back out.
- **Fluid** — powered grow beds and the greenhouse controller accept Nutrient fluid.

## Faces and direction

Each face of a machine can be set to pull, push, or stay idle for a channel. Because item groups are split
into input and output, a hopper under a Harvester pulls only finished produce, and a hopper feeding a
Planter inserts only into seed slots — the machine will reject items that do not belong in that group. Grow
beds and the greenhouse controller accept energy and fluid but never output them.

See also: [Automation](Automation.md), [Grow Beds](Grow-Beds.md).
