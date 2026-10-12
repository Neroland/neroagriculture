# Crafting recipes

Every NeroAgriculture block and base item is craftable in survival on a vanilla crafting table. Recipes
carry **no progression gate**; the fragment ladder paces them, because each tier's recipes want
fragments of that tier. Letters below are grid positions, read left to right, top to bottom.
`I` = Iron Ingot, `R` = Redstone Dust, `T` = Territe Fragment, `F` = Forgite Fragment,
`O` = Orbite Fragment.

## Start of the ladder (vanilla parts only)

| Result | Pattern | Key |
| --- | --- | --- |
| Fragment Extractor | `IPI` / `CFC` / `IRI` | `P` Piston, `C` Copper Ingot, `F` Furnace |
| Territe Grow Bed | `ICI` / `DDD` / `III` | `C` Composter, `D` any dirt block |

Feed the extractor coal, iron or another tier-1 ore and it yields Territe Fragments, the first rung.

## Territe tier

| Result | Pattern | Key |
| --- | --- | --- |
| Fragment Infuser | `TRT` / `IBI` / `TRT` | `B` Blast Furnace |
| Seed Synthesizer | `TST` / `ICI` / `IRI` | `S` Wheat Seeds, `C` Crafting Table |
| Seed Research Bench | `TBT` / `ILI` / `III` | `B` Book, `L` Lectern |
| Planter | `ISI` / `IDI` / `TRT` | `S` Wheat Seeds, `D` Dispenser |
| Harvester | `IHI` / `IDI` / `TRT` | `H` Iron Hoe, `D` Dispenser |
| Fertiliser Applicator | `IFI` / `IDI` / `TRT` | `F` Fertiliser, `D` Dispenser |
| Fertiliser Processor | `IBI` / `ICI` / `TRT` | `B` Bone Meal, `C` Composter |
| Greenhouse Frame (x8) | `ITI` / `T T` / `ITI` | |
| Greenhouse Glass (x8) | `GGG` / `GTG` / `GGG` | `G` Glass |
| Greenhouse Door (x3) | `GG` / `GG` / `FF` | `G` Greenhouse Glass, `F` Greenhouse Frame |
| Greenhouse Controller | `TGT` / `ICI` / `IRI` | `G` Greenhouse Glass, `C` Redstone Comparator |
| Crop Tower Frame (x4) | `I I` / `ITI` / `I I` | |
| Crop Tower Controller | `TFT` / `IHI` / `IRI` | `F` Crop Tower Frame, `H` Hopper |
| Speed Module | `TRT` / `RCR` / `TRT` | `C` Clock |
| Efficiency Module | `TLT` / `LCL` / `TLT` | `L` Lapis Lazuli, `C` Redstone Comparator |
| Prospora Seed | shapeless | Wheat Seeds + 2 Territe Fragment |
| Blank Seed | shapeless | Wheat Seeds + Paper + Territe Fragment |

## Forgite tier

| Result | Pattern | Key |
| --- | --- | --- |
| Genetics Station | `FGF` / `IBI` / `IRI` | `G` Glass, `B` Brewing Stand |
| Pollination Beacon | `FHF` / `IGI` / `IRI` | `H` Honeycomb, `G` Glowstone Dust |
| Oxygen Plant | `FKF` / `IGI` / `IRI` | `K` Kelp, `G` Glass |
| Biofuel Converter | `FMF` / `ICI` / `IRI` | `M` Biomass, `C` Cauldron |

## Orbite tier

| Result | Pattern | Key |
| --- | --- | --- |
| Terraforming Controller | `OGO` / `ICI` / `IRI` | `G` Glowstone, `C` Greenhouse Controller |
| Terraforming Seed | ` O ` / `BPB` / ` O ` | `B` Bone Meal, `P` Prospora Seed |

## Grow beds

Each bed is the bed below it surrounded by four fragments of the new tier, in a plus shape:
Forgite Grow Bed from a Territe Grow Bed, Orbite from Forgite, Colonite from Orbite, Voidite from
Colonite.

## Consumables

Fertiliser, Speed Fertiliser, Yield Fertiliser, Biomass and Crop Waste have simple vanilla-ingredient
recipes; see [Fertiliser](Fertiliser.md). The five tier fragments compact 9:1 into blocks and back; see
[Biofuel and fragment blocks](Biofuel-and-Fragment-Blocks.md). Resource seeds, charged seeds and
fragment conversions are machine recipes; see [Fabrication chain](Fabrication-Chain.md) and the
[Recipe viewers](Recipe-Viewers.md) page for JEI/EMI.
