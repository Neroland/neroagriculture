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
   default conversion is 8–24 fragments per item. Resource crops now advance two stages per random tick
   at a neutral cycle. These are code defaults, not measured play times.
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

## Resource audit notes

- Nerotech's iron, copper and gold dusts duplicate the vanilla metal loop; its power cells, circuits and
  machine components are crafted technology, so they are not direct seed outputs. The existing vanilla
  metal seeds support their constituent materials without replacing those recipes.
- Core's Nero Alloy, Starsteel and Void Crystal dusts are actual meteor drops, not crafted devices; the
  built-in seeds output the dust and still require a real sample and research. Their conversion and
  progression need further balance testing.
- NeroCreatures' worm chitin, ore slurry, refined crystal and void essence are registered creature drops.
  Their tags, quest objectives and economy commodity entries were reviewed below.
- NeroPower supplies an empty optional uranium-raw-material tag and crafts its uranium pellets from
  other ingredients; it does not register a raw uranium item. Pellets and fuel rods remain crafted,
  avoiding a direct shortcut through the reactor fuel chain.
- The current NeroLogistics, NeroColonies, NeroQuests, NeroNotes and NeroSecurity item registrations
  predominantly expose devices, upgrades, books, disks and credentials. These are excluded as direct
  resource-seed outputs. NeroEvents' salvage plating, conduit and core fragments are event rewards;
  they remain event loot rather than default direct-seed outputs.

- Nerospace supplies seven explicit optional materials: raw Nerosium, raw Nerosteel, Xertz Quartz,
  Cindrite, Glacite, raw Hematite and Voidglass. Hematite is used for iron, radiation lining and
  pigment; Voidglass is used for lenses and prisms. Sulfur makes gunpowder, which already has a seed;
  raw Ilmenite has no identified use in Nerospace recipes. Alien technology and machine parts remain
  outside the direct-resource list. This inventory does not establish late-game balance.
- NeroFactions registers a trade terminal; NeroEconomy registers terminals, a handbook and credit chip;
  NeroDecor registers decorative block items; NeroArcade registers chips, cartridges and arcade blocks.
  These items are not candidate natural resource outputs. No item registry was found in the inspected
  common sources of NeroRuins, NeroCompanion or NeroLink; revisit when their inventories change.
- NeroEvents' three salvage items appear in wreck, pod and crater loot or debris rewards. They are not
  given default seed recipes because they are event rewards. NeroCreatures also has
  plasma cells, contraband, salvaged circuitry, an android core and an apex trophy in addition to its
  organic and mineral drops. Stalker hide and sinew already have optional definitions; other creature
  drops are not given new default seed recipes in this pass. The common `c:dusts/ore_slurry` and
  `c:gems/refined_crystal` tags make those two candidates eligible for automatic catalog discovery at
  the default Orbite tier.
  Discovery alone does not provide their extraction or seed-synthesis recipes: the conservative fallback
  admits only explicit definitions and selected common metals. Economy classifies slurry as farmable
  (reference price 4, daily demand 128), refined crystal as a staple (35, demand 32), worm chitin as a
  staple (25, demand 32), and void essence as farmable (150, demand 8, two sales per account daily).
  Crystal and essence are also quest collection targets, each paired with a creature-kill objective.
  Existing hide and sinew definitions remain; the other drops retain their current recipe behavior.
  Revisit new default recipes after measuring crop throughput and economy effects.

## NeoForge 26.3 verification notes

- The client loaded the optional Nerospace 2.0.1, NeroQuests and NeroColonies test jars together.
  Cindrite was rejected for planting in the Overworld and auto-planted from a Colonite bed's seed slot
  on Cindara. This confirms the dimension check and slot-driven planting, not greenhouse growth or
  hand planting on a powered bed.
- Both engineered food and alien produce matched NeroColonies' colony-food tag. The Field Ledger
  loaded in NeroQuests and its first collection objective advanced. These are targeted checks, not
  a complete colony feeding or survival playthrough.
- A command-assisted machine test used a coal-fuelled Field Generator to power extraction of one
  coal into two Territe fragments and one coal fragment. The research bench consumed coal and
  reported success; an empty research attempt reported failure. A powered, owner-claimed synthesizer
  made a coal seed from coal, two Territe fragments and Prospora, then converted eight supplied coal
  fragments into one coal. The inputs were supplied by commands, not gathered in survival.
- A command-built 27-block greenhouse with a controller in the outside wall reported unpowered,
  formed with generator power, breached when a wall block was removed, and recovered after repair.
  With command-supplied nutrient and bed energy and accelerated random ticks, an Orbite diamond
  crop grew and auto-harvested into its bed. After saving and reloading, the greenhouse re-formed
  and the crop history, bed contents and resource buffers persisted. This is not a normal-rate
  balance measurement.
- A command-built three-layer tower with one coal seed and supplied energy and nutrient harvested
  repeatedly. Removing a middle frame stopped its crop and resource use; replacing it resumed the
  same crop and harvest history. After saving and reloading, the crop, history, output and buffers
  persisted. This is a targeted structure test, not a throughput balance test.
- With Nerospace 2.0.1 present, a tower accepted Cindrite on Cindara; an Overworld tower retained the
  same seed in its input and displayed the wrong-dimension status. The test used supplied materials.
- A command-built Mars tower accepted a supplied Hematite seed, but initially stayed at age zero despite
  full power and a growing status: the hostile-world cost exceeded the external energy transfer limit.
  After fixing internal consumption and the status calculation, the saved tower produced at least three
  Hematite fragments over 400 sprinted game ticks. At 80 NF, below the 160 NF hostile growth cost, it
  reported insufficient power. The structure, energy, nutrient and seed were supplied by commands.
- A command-built Nyxara tower accepted a supplied Voidglass seed and harvested at least four fragments
  with supplied energy and nutrient. A separate powered, claimed synthesizer retained 23 supplied
  Voidglass fragments, then consumed 24 and produced Voidglass. These checks do not establish seed
  synthesis, natural material gathering or a complete survival playthrough.
- The same transfer-limit error was corrected in grow beds, greenhouse upkeep, area machines and
  terraforming controllers. The 26.3 unit test covers a bed cost above its external transfer rate and
  checks that insufficient resources consume neither energy nor nutrient. In the 26.3 client, a
  command-supplied Forgite bed at 160 NF per growth step (80 NF external transfer limit) grew and
  harvested an iron fragment. Its buffers fell from 1,000 to 40 NF and from 1,000 to 850 mB nutrient.
  This used accelerated random ticks; normal tick speed and the original client config were restored.
  In-game regression of greenhouse, area-machine and terraforming paths at elevated costs remains open.
- In the combined Nerospace, NeroQuests and NeroColonies client, a command-supplied raw Hematite
  sample was retained when research was attempted before visiting Mars. After teleporting to Mars and
  back, the same research action reported complete and consumed the sample. This confirms the visit
  gate in a command-assisted test, not an unassisted survival journey. The initial blocked message was
  obscured by the research button; the button was moved below the status line. The rebuilt screen
  displayed the entire message and its repositioned button worked. A Voidglass sample likewise stayed
  in the bench before a Nyxara visit and was consumed after the visit. Command-supplied Hematite and
  Voidglass seed models were also inspected as dropped items in this client.
- With Nerospace 2.0.1 present, the live catalog reported Mars Hematite and Nyxara Voidglass as active
  with their intended outputs, tiers and world restrictions. A Core-only client loaded successfully and
  reported Voidglass absent; unit tests cover both missing source items. Research and growth of these
  two additions have not been played through in survival.
- A command-created early Hematite crop and mature Hematite and Voidglass crops rendered on their
  grow beds in the 26.3 client; the Voidglass block entity reported the correct identity and tier.
  Both mature crops initially appeared very dark because their green textures were multiplied by the
  material tint. After neutralising the sprites, the same fixtures showed rust-red Hematite and violet
  Voidglass in daylight. All eight alpha masks and dimensions matched the original sprites. These fixtures
  were placed in the Overworld and do not prove natural planting or growth on their required planets.
- Component-backed Hematite and Voidglass seed and fragment models were inspected in the 26.3 client
  without Nerospace installed. A command-prepared iron crop retained its material, tier, harvest count
  and yield/speed/hardiness traits across a save and reload; random tick speed was restored afterward.
- A command-created mature Cindrite crop in the Overworld was left intact by its grow bed, manual
  interaction and a powered area harvester. The same harvester collected an allowed mature coal crop,
  used 60 NF and deposited two fragments. These checks used supplied blocks and crop data.
- After the Territe growth adjustment, a command-created coal crop reached maturity on a passive bed
  in the 26.3 client with accelerated random ticks and artificial light. The tick speed was restored
  afterwards. This confirms growth still works, not the time needed in ordinary survival.
- A Territe bed placed before the bed-screen change opened after updating, including when using its
  side while holding a resource seed. Its existing coal crop auto-harvested into the new output inventory
  with accelerated ticks; the inventory and planted crop persisted after a save and reload. Normal tick
  speed was restored. This does not verify the complete survival progression.
- With the same Territe bed empty, a slotted gold seed was retained with a higher-tier warning; a coal
  seed was accepted and planted. The revised status and layout were inspected in the 26.3 client.
- After reloading datapacks in the isolated world, a command-supplied iron ingot awarded the Field
  Generator recipe advancement. This verifies its unlock trigger, not acquisition in survival.
- Hand-applied speed fertiliser displayed confirmation and wrote a dose to a Territe bed. In survival
  mode, a two-item stack became one after application; the test used supplied items in an isolated world.
- At the default random tick speed of 3, a command-placed iron crop on a supplied Forgite bed harvested
  one fragment. The bed fell from 1,000 to 840 NF and from 1,000 to 900 mB nutrient. The harvest was
  observed within approximately 5 minutes of a nearby game-time reading; a light was added during the
  run. This is one sample, not a measured average or survival playthrough.
- The 26.3 build, unit tests and ECJ analysis pass. Coal seed hand planting on a Forgite bed was
  verified in the client after aligning the bed's selection outline with its visible top. Full
  first-loop survival play, normal-rate balance measurements and extended automation remain open.
