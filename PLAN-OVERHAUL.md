# Resource farming overhaul

Baseline: 0.6.1-beta.1, Core 1.17.0; nine Minecraft versions on three loaders. The project
instructions' skeleton description is obsolete. This is a partial source audit, not runtime validation.

## First loop and confirmed gaps

1. A crafting table makes the extractor from iron, copper, redstone, piston and furnace. Coal is
   its Territe sample; the authored recipe consumes one and uses 800 NF over 80 ticks to give two
   Territe fragments and a material fragment. The new Field Generator supplies starter power from coal
   or charcoal; the complete survival loop still needs runtime verification.
2. Two Territe fragments and wheat seeds make Prospora. It grows on farmland and mature harvest yields
   one seed and 1–2 Territe fragments. A Territe bed, research bench and synthesizer have crafting recipes.
3. The bench consumes a coal sample to record research. The synthesizer consumes coal, two
   Territe fragments and Prospora, with power. Plant the seed on a Territe bed, harvest material
   fragments, and convert eight into coal in the powered synthesizer. Verify this path in a client,
   including discovery, crop drops and recipe matching.
4. The catalog has 15 built-ins, six organic definitions, optional sibling definitions and tag discovery.
   Explicit definitions and selected common metals have catalog-backed fabrication; other discovered
   entries still need recipes. Fusion now enforces target discovery and research.
5. Tier fragments upgrade 4:1; fresh yield is normally one fragment (two for coal), ramping to 3–7 over 32–160 harvests;
   default conversion is 8–24 fragments per item. These are code defaults, not measured play times.
   Higher beds need NF and nutrient; Orbite and above require a greenhouse by default.

## Milestones

1. Supply a balanced Core-only survival power bootstrap; validate the complete first coal loop and
   communicate it in game. Cover missing inputs, power loss and reloads.
2. Make catalog-backed extraction, research and synthesis work safely for eligible definitions;
   preserve explicit recipes and pack overrides. Test duplicates, blacklists and absent optional items.
3. Audit sibling registries and progression individually, then define optional native materials,
   planetary visit requirements and deliberate exclusions. Test common, rare and alloy cases.
4. Measure growth and resource economy, publish a five-tier balance table, and address farm, genetics,
   automation and reload exploits with persistence and conservation tests.
5. Implement distinct curated resource art and scalable visual families, inventory the assets, and
   inspect item and crop appearances in an actual client on supported loaders.
6. Update guidance, recipe viewers, configuration and public wiki; run NeoForge 26.3 builds, tests, analysis,
   resource validation and survival smoke tests as the primary test target. Preserve component, block entity and saved-data formats;
   document and test any necessary migration.

No milestone is complete until its tests and runtime checks pass. In particular, no native ecosystem
coverage, balance, save compatibility, or visual completion is claimed by this baseline.
