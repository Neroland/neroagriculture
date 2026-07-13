# Fabrication Chain

Stage 5 connects physical resource samples to repeatable cultivation without trusting client item data.
All three processing machines use Neroland Core energy storage, upgrades, and item/energy Side Config.
Their five-slot inventory is exposed through the native automation capability on every loader.

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

The Synthesizer uses three inputs: a real material sample, matching Resource Fragment, and a Charged Seed
of the resolved catalog tier. It also requires the player's Core material-discovery and Agriculture
research milestones. The server rechecks the recipe, catalog, gate, milestones, components, output space,
and physical inputs before producing a fresh Resource Seed.

Putting Resource Fragment in the first slot selects a conservative material-conversion recipe instead.
The recipe must name the same material, meet its catalog conversion cost, and output the catalog's exact
resource item.

Player- or team-scoped checks use an eligible player within 16 blocks; no player UUID is stored on the
machine. The screen shows progress, NF, and explicit blocked states such as no power, closed gate, missing
research, invalid components, or full output.
