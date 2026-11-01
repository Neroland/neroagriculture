# Fabrication Chain

Stage 5 connects physical resource samples to repeatable cultivation without trusting client item data.
All three processing machines use Neroland Core energy storage, upgrades, and item/energy Side Config.
Their five-slot inventory is exposed through the native automation capability on every loader.

## Field Generator

Craft the Field Generator from iron, copper, a furnace and redstone. Place it next to a processing
machine and insert coal or charcoal into its first slot. Each fuel produces 3,200 NF at 8 NF per tick;
the generator pauses when its buffer is full and resumes without consuming another fuel. Energy output
uses Core's side configuration, and its fuel and remaining burn time persist across reloads.

## Fragment Extractor

The Extractor accepts a real sample in its first input. It resolves one deterministic material from the
current server catalog, then matches a `material_extraction` datapack recipe. Only after the recipe,
energy, and both output slots remain valid does it consume the sample. It produces neutral tier fragment
and a fresh Resource Fragment stack whose material and tier are rebuilt from the server catalog.

## Fragment Infuser

The Infuser performs the default 4:1 condensation steps from Territe through Voidite fragment. Supplying a
Blank Seed in its second input instead selects a charging recipe. The output Charged Seed carries only a
versioned tier component. Destination gates are checked when work begins and again at completion.

## Seed Synthesizer

The Synthesizer uses three inputs: a real material sample (first slot), enough matching neutral **Tier
Fragments** of the material's tier (second slot), and a **Prospora Seed** base (third slot). It also
requires the player's Core material-discovery and Agriculture research milestones. The server rechecks the
recipe, catalog, gate, milestones, components, output space, and physical inputs before producing a fresh
Resource Seed. Hoppers and pipes can feed all three inputs — the machine's slot filter accepts exactly
what the recipes consume (Tier Fragments in the second slot, Prospora Seeds in the third).

Putting Resource Fragment in the first slot selects a conservative material-conversion recipe instead.
The recipe must name the same material, meet its catalog conversion cost, and output the catalog's exact
resource item.

Player- or team-scoped checks (gates, research milestones, tier unlocks) are **owner-bound**: each
machine records its placer's UUID (opt-out via `automation.track_owner`; server-only, erasure-aware —
see [Privacy](Privacy-and-Erasure.md)) and credits that player. Player-bound fabrication pauses if that
owner is offline or owner tracking is disabled. Opening an older ownerless machine claims it when owner
tracking is enabled. The screen shows progress,
NF, and explicit blocked states such as no power, closed gate, missing research, invalid components, or
full output.

Removing a machine by any means — survival or creative break, or an explosion — drops its inventory,
including installed upgrade modules; nothing inside is ever voided.
