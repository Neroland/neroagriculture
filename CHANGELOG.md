# Changelog

All notable changes to **NeroAgriculture** are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### 2026-07-29 UX additions (build-verified across the six cells)

#### Added

- **In-GUI build guides**: the Crop Tower Controller and Greenhouse Controller screens gained a
  left-hand guide panel (scrollable, with a small block-layer sketch) walking through how to build
  the structure — tower: controller + 3–12 stacked frames, power/nutrient, seeds; greenhouse:
  sealed shell, no gaps, Greenhouse Door airlock, powered controller, volume/upkeep notes.

#### Fixed (UI)

- **Crop Tower screen layout**: the tower readout no longer collides with the output slots; the
  screen grew taller with dedicated rows for gauges, growth bar (now always visible, "Growth —"
  when empty), tower readout, and status line; the seed panel's truncated heading lines were
  shortened to fit ("All tiers grow" / "Gates at growth").

#### Added (gameplay)

- **Growth progress readouts**: Grow Bed screens show a live growth bar with percentage for the
  planted crop; Crop Tower screens show average growth plus a "Ripe n/m" mature-slot counter
  (aggregate — towers are config-sized). Synced as permille gauge data alongside the existing
  energy/fluid gauges.
- **Greenhouse Door** — a two-block metal-and-glass door in the greenhouse aesthetic that acts as an
  airlock: it counts as sealed shell whether open or closed, so players can enter without breaching
  the greenhouse. Hand-openable, crafted 2×3 from Greenhouse Glass over Greenhouse Frame (yields 3),
  drops itself, listed in the creative tab, and demonstrated in the `/neroagriculture gallery` dome.

#### Fixed

- **Gallery: the greenhouse controller is clickable again** — the showcase's Creative Battery was
  placed on the controller's only exposed face; it now powers it from below the wall, and the dome
  gained a Greenhouse Door so the exhibit can be walked into.

### 2026-07-29 audit remediation (build-verified across the six cells)

Full code + design audit follow-up (`neroland-mc-ecosystem/audits/2026-07-28-neroagriculture-audit.md`).

#### Fixed

- **Recipe lookup no longer scans and sorts the whole server recipe manager every tick** — per-type
  id-sorted cache plus an input-dirty/20-tick idle gate on fabrication machines; material matching is
  cached per resolved catalog. (H1)
- **Breaking a Grow Bed or Foundation Machine no longer voids its inventory**; all machines now drop
  contents from the block-removal hook, covering creative breaks and explosions too. (H2)
- **Resource/species crop block entities sync to the client** — in-world crop tints now render the
  real material colour instead of the fallback; grow beds no longer broadcast a full-NBT update
  packet on every dirty-mark. (H3, M4)
- **Server-scoped static caches (terraforming regions, greenhouse index, cycles, material catalog,
  gallery records, visit tracking) are cleared on server stop** instead of leaking into the next
  single-player world. (H4)
- **Forge capability layer now honours per-side machine side-config dynamically**, matching
  Fabric/NeoForge semantics. (H5)
- **Platform services are preloaded at mod construction** even when telemetry is opted out,
  eliminating a lazy mid-tick ServiceLoader crash class. (M1)
- **Player-data erasure now reaches machines in unloaded chunks** via a persisted tombstone set with
  a configurable retention window (`privacy.erasure_retention_days`, default 90). (M2, POPIA/GDPR)
- **Greenhouse oxygen contributions are retracted** on breach/unpower/removal. (M9)
- Seed Synthesizer hopper filter matches its actual recipe inputs; crop-tower harvests no longer
  burn fertiliser when the output is jammed and split yields across stacks (all-or-nothing);
  dedicated-server clients resolve materials/species via the synced client catalog; gauge values sync
  as permille of live capacity (no more hard-coded maxima or 16-bit truncation); compatibility panels
  are cached per screen; plus the full low-severity sweep (menu slot filters, parser hardening,
  unified colour parsing, aggregated reload logging, gallery clear made surgical, network action
  validation, Fabric dedicated-server classloading, zero-yield age reset, client cache clearing on
  disconnect). (M5–M8, M11, Low)

#### Changed

- **`progression.require_research` now defaults to `true`** — the Seed Research Bench is part of the
  default loop; hard tier gates remain off by default (2026-07-16 decision) and sibling overlays
  remain opt-in. (D1)
- **Crop towers now evaluate per-layer climate** and pay a configurable NeroFlux surcharge in hostile
  environments (`crop_tower.hostile_environment_nf_multiplier`, default 4) instead of bypassing the
  environment system. (D3)
- **Unknown discovered materials default to Orbite** (was Forgite), restoring the documented
  anti-inflation stance; configurable via `discovery.default_tier`. (D4)
- **Gate unlocks and machine/crop/pollination gate checks are owner-bound** (placer-recorded,
  erasure-covered), with nearest-player fallback only for ownerless blocks. (D5)
- Terraforming controllers record their owner only after authorization succeeds.

#### Added

- **Nerospace planet-visit adapter** (runtime-guarded, no hard dependency): planet visits grant
  Core `material_discovered` milestones for planet-bound materials, making Nerospace-restricted
  materials researchable; historical visits backfill reflectively when Nerospace ≥ 1.0.0-beta.8 is
  present. Toggle: `compat.nerospace_visits` (default on). Live tracking is event-driven on
  Forge/NeoForge and tick-diffed on Fabric.
- Consolidated cross-loader tables: canonical machine block-entity list and screen-binding list in
  `common` consumed by all loaders — new machines can no longer silently miss capabilities/screens.

---

Resource & progression rework — reframing the mod around a resource-seed economy with standalone,
NeroAgriculture-native progression. In progress; each stage is build-verified across the six cells.

### Changed

**Stage 1 — catalog & tiers**

- Tier gating is now **standalone and native**. The former sibling-mod tier gates
  (`nerolandcore:industrial_power` / `reached_orbit` / `first_colony` / `deep_space`) are replaced by
  NeroAgriculture-owned gates `refinement` (T2), `synthesis` (T3), `transmutation` (T4) and
  `ascension` (T5); tier 1 is open from the start. Gates ship as datapack definitions under
  `data/neroagriculture/neroland_gates/` (per-player scope, linear prerequisites) and the mod opens
  them itself, so all five tiers are reachable with only Neroland Core present.
- **Tag-driven auto-generation** now covers `c:ingots/*`, `c:gems/*`, `c:ores/*`, `c:dusts/*` and
  `c:raw_materials/*` (previously ores only), assigning each discovered resource a tier via a
  documented heuristic and a resolved colour, with the preferred output picked per tag category.
  Curated built-ins and datapack definitions still take precedence; recipes require the real
  resource, so absent mods produce no broken entries.
- **Config per-resource colour override** (`color=#RRGGBB`) added alongside the existing
  tier/gate/yield/conversion/enabled overrides.

**Stage 2 — the fragment ladder (no more "essence")**

- The word **"essence" is gone** — everything is now a **fragment**. The five tier items are renamed
  to the Cosmic-ascent ladder **Territe → Forgite → Orbite → Colonite → Voidite Fragment**; the
  per-resource drop is the **Resource Fragment**; the machines are the **Fragment Extractor** and
  **Fragment Infuser**; the affinity enum is `FragmentTier`; and the infusing recipe type is
  `neroagriculture:fragment_infusing`. Blocks, tags (`c:fragments`, `c:fragment_blocks`), models,
  textures, loot, recipes and lang were renamed to match.
- The **fragment-upgrade ladder** (4× tier-N → 1× tier-N+1, via the Fragment Infuser) carries steeply
  scaling NeroFlux + time costs up the ladder.
- **Native gates now open themselves.** Producing a fragment of a tier opens the next tier's gate for
  the nearby player (Territe → Refinement, Forgite → Synthesis, Orbite → Transmutation, Colonite →
  Ascension), respecting each gate's prerequisites. Tier 1 extraction is ungated, so the whole ladder
  is reachable from a fresh start with only Neroland Core.

**Stage 3 — resource seeds & the Prospora base**

- New **Prospora Seed** base item — the crafting core of every resource seed. Craftable from Territe
  Fragments + wheat seeds, closing the standalone loop.
- **Resource-seed synthesis reworked** to *require the real resource*: a Resource Seed is now
  synthesized from the real resource + N matching Tier Fragments (N scales with tier) + one Prospora
  Seed, replacing the old charged-seed bootstrap. Seeds stay an amplifier, never a way to obtain a
  resource you have never seen; absent mods simply yield no craftable seed.
- **Dynamic creative tab** — the tab now lists one Resource Seed per resource in the (client-synced)
  material catalog, so it reads like a distinct seed per resource for vanilla and any modded resource,
  with the curated examples shown as a fallback before the catalog syncs.
- **Self-sustaining farms** — harvesting a mature resource crop now has a configurable chance
  (`growth.seed_return_percent`, default 10%) to also return a planted seed, carrying its material tint
  and genetics.
- **Prospora base crop** — the Prospora Seed now also plants a **base crop** on farmland (a simple
  wheat-style crop): it grows and, on harvest, drops **Territe Fragments** plus a Prospora Seed, a
  renewable standalone entry to the ladder (Territe is also obtainable from tier-1 ore extraction, so
  this is a convenience, not a gate).
- **In-world crop block tint** — placed Resource Crops now tint to their resource's ingot colour, via a
  custom `BlockTintSource` that reads the crop block entity's material (resolved through the shared
  `MaterialColors`). Wired on all three loaders — each exposes vanilla `BlockColors.register` a little
  differently (NeoForge `RegisterColorHandlersEvent.BlockTintSources`, Forge
  `RegisterColorHandlersEvent.Block.getBlockColors()`, Fabric via `Minecraft.getBlockColors()` directly,
  no Fabric-API rendering module) — and the crop models gained a `tintindex` (a shared `tinted_crop`
  parent, kept in lockstep in `gen_textures.py`).

> Everything above builds green across all six cells; the machine UIs and the two tint paths are
> compile-verified but still want an in-client look at playtest (in particular, that Fabric can read
> `getBlockColors()` at client-init time).

**Stage 4 — fusion / alloy seeds**

- New **data-driven fusion recipe type** `neroagriculture:fragment_fusion`, run on the Fragment
  Infuser: it combines a primary Resource Fragment with a second input (item or tag) into a combined
  **alloy seed**, without requiring the alloy resource itself. Fields: `material` (primary fragment's
  resource), `input_count`, `secondary` + `secondary_count`, and `result_material` (the alloy). Fully
  datapack-extendable.
- **Curated built-in alloys:** Steel (Iron Fragment + Coal), Energized Steel (Steel Fragment +
  Redstone), and a Core-native **Nero Alloy** (Iron Fragment + Redstone) that works standalone since
  Core always provides Nero Alloy. Steel/Energized Steel are **dormant-safe** — they only activate when
  a mod supplies the alloy resource, and produce no broken recipe when absent.
- The alloy seed inherits the alloy resource's **own catalog tier**, so the downstream grow → Resource
  Fragments → resource loop stays consistent; a `fusion.max_tier` config clamp (plus `fusion.enabled`)
  bounds which alloys may be fused. Multiple alloys sharing a primary fragment are disambiguated by
  their second input.

**Stage 5 — optional cross-mod sibling overlays**

- Higher tiers can now *additionally* require the matching Neroland arc gate that a sibling mod drives
  open — Forgite/Refinement ↔ `industrial_power` (Nerotech), Orbite/Synthesis ↔ `reached_orbit`
  (Nerospace), Colonite/Transmutation ↔ `first_colony` (NeroColonies), Voidite/Ascension ↔
  `deep_space` (Nerospace) — giving modpacks the cross-mod "link" feel.
- Controlled by `progression.sibling_overlays` = **auto** (default) / on / off. In auto, an overlay
  applies **only when the sibling mod that can open that arc gate is loaded**, so a Core-only game is
  never gated by an arc gate nothing could open — standalone play is never blocked. The overlay is
  layered onto every resource-tier gate check (machines, planting, growth, harvest, automation, towers)
  and is dormant-safe when the sibling is absent.

**Stage 6 — resource-colour tinting (functional core)**

- Resource **seeds and fragments now tint to their resource's ingot colour**. The colour is baked into
  the vanilla `custom_model_data` component when each stack is created (extraction, synthesis, fusion,
  harvest, crop-return, towers, creative tab), and the item model's `tints` reads it through the
  built-in `minecraft:custom_model_data` tint source — so tinting is **loader-agnostic** (no per-loader
  colour handler) and behaves identically on Fabric, Forge and NeoForge. Colour comes from the shared
  `MaterialColors` resolver, matching the catalog.

- **Texture repaint applied.** `tools/gen_textures.py` was resynced to the fragment/Cosmic-ascent
  naming, its lab palette **darkened ~15%** (kept the clean look), and it now emits **neutral greyscale
  bases** for the tinted resource seed and fragment so their `custom_model_data` tint multiplies to the
  resource colour cleanly; the full set was regenerated. Verified programmatically (palette luminance
  dropped as intended, tint bases have zero channel spread, no old-named assets produced).

> Still deferred to a dev-machine step (needs a running client — a custom block tint source can't be
> introspected or rendered here): the **in-world crop block tint** so placed crops show the resource
> colour. Tracked in the follow-on task, alongside the optional Prospora base crop and seed-return
> tuning.

**Stage 7 — balance, food de-emphasis, docs, verification**

- **Wiki updated** to the fragment/Cosmic-ascent terminology throughout (the two "Essence"-named pages
  became `Resource-Fragment.md` and `Biofuel-and-Fragment-Blocks.md`), with new pages for the
  resource-seed economy, fusion and alloy seeds, and sibling overlays, and a rewritten `Home.md`
  that leads with the core loop. Old sibling gate ids in doc examples were replaced with the native
  gates. The word "essence" now appears nowhere in the mod's code or wiki.
- **Food & alien** content is de-emphasised to a clearly-labelled secondary side-system in the docs and
  ordered after resource seeds in the creative tab; nothing was removed.
- **Balance** was kept conservative: the ladder's steep per-tier NeroFlux/time costs, the tier-scaled
  seed fragment cost, and the fusion costs all preserve Core's net-conservation invariant (no single
  harvest can mint a resource), which the `TierBalance` tests continue to assert.

### Machine UIs

Every machine that previously reported its state only via chat now opens a real screen. All follow the
existing texture-free, `fill`-drawn style, with menu types + per-loader screen registration wired.

- **Genetics Station** — two input slots (seed / seed + fragment), a locked output, energy + splice
  progress, and a live readout of the input seed's traits (read off the synced slot).
- **Crop Tower Controller** — three seed slots + a fertiliser slot, six output slots, energy + nutrient
  gauges, and a tower height / active-slots readout.
- **Planter / Harvester / Fertiliser Applicator** — a 3x3 seed/output grid, three upgrade slots, energy,
  and a mode + working-range readout (one menu for all three of the shared area machine's modes).
- **Greenhouse**, **Terraforming**, and **Pollination Beacon** — read-only status panels (via one shared,
  slot-free status menu keyed by a synced machine id) showing state/volume/crops, progress/stage, or
  range respectively.

(All compile-verified across the six cells on Fabric/Forge/NeoForge; the visual layout and in-game
behaviour still want an in-client check on the dev machine.)

### Playtest round 1 fixes (2026-07-15 screenshots)

- **Seeds and fragments are named for their resource** — "Iron Seed", "Redstone Fragment" — built
  from the material component (`%s Seed` / `%s Fragment` lang keys, title-cased material path).
- **Block items translate again**: all block items now use their block's `block.*` translation key
  (26.x `useBlockDescriptionPrefix()`), fixing raw keys like `item.neroagriculture.oxygen_plant`.
- **Fluids have textures**: new `nutrient_still/flow` and `biofuel_still/flow` sprites, and the fluid
  models are registered on **NeoForge** (`RegisterFluidModelsEvent`) — this was the magenta/black
  checkerboard "liquid" (and the untextured source-block shape). *Forge and Fabric followed on
  2026-07-25; see "Fluid rendering on Forge and Fabric" below.*
- **Fabrication screens un-squished**: the shared machine screen grew to 176x172 with banded layout —
  energy gauge, slot row, status line, progress bar and the research pill no longer overlap.
- **Bioreactor, Biofuel Converter and Fertiliser Processor are now interactable** with real UIs (a
  shared processor menu/screen: inputs → progress arrow → output + energy).
- **Grow beds have a UI**: a seed slot (auto-plants above the bed once a second, same catalog/tier/
  gate checks as hand-planting, gates checked against the nearest player), energy + nutrient gauges,
  and the bed tier. Hoppers can feed the seed slot.
- **Crops no longer float** above grow beds: the crop cross models are shifted down so bed crops
  emerge from the grow-bed soil (`tinted_crop` for resource crops, new `bed_crop` parent for
  food/alien crops; `gen_textures.py` kept in lockstep). The Prospora farmland crop keeps the vanilla
  height.

### Playtest round 2 fixes (2026-07-16 screenshots)

- **No more hard gates.** Tier progression gates no longer block anything by default: material
  definitions get no default gate, machine steps are never gate-blocked, and sibling overlays default
  **off**. The Seed Research requirement is also off by default (`progression.require_research`).
  The native gate ids remain registered (and still open via milestones) so packs can explicitly gate
  materials via datapack/config if they want unlocks back.
- **Machine screen overlap fixed for real**: the fabrication machines now use a single clean slot row
  — inputs | upgrades | outputs — with the status line and progress bar in their own bands below.
- **Right-click with an item in hand now opens machine UIs** (previously vanilla would place the held
  block instead — which made machines look like they "had no UI" when testing with a full hotbar).
  Applies to every machine including Planter/Harvester/Applicator; grow beds still hand-plant seeds.
- **Working-area hologram toggle**: the Planter/Harvester/Fertiliser Applicator UI gains a
  "Show area" button that outlines the machine's working radius in-world with end-rod particles
  (server-side, cross-loader; persisted per machine).
- **Fluids animate**: nutrient/biofuel still+flow sprites are now 8-frame animated strips with
  `.mcmeta` (shimmering still, scrolling flow).

### Playtest round 3 (2026-07-16 requests)

- **Grow beds are hopping pots**: a bed now auto-harvests its mature crop into **its own four output
  slots** (hopper-extractable) and auto-replants from its seed slot — the full farm loop automates on
  the bed itself. Bed UI shows seed slot → output slots; side config exposes seed in / harvest out.
- **Planter/Harvester/Applicator base area is 7x7** (3 blocks on either side), range upgrades now
  extend it up to 13x13.
- **Area-machine UI overlap fixed** (Fertiliser Applicator screenshot): upgrades moved to the right
  edge (x152) and the middle band stacks mode / range / "Show area" pill / energy on separate lines.
  Same sweep tightened the other screens — compact tower status ("H12 · 48 slots"), two short lines on
  the grow bed, the research pill nudged clear of the status text, genetics trait line raised 2px.
- **Crops are walk-through**: all four crop blocks (resource, Prospora, food, alien) now use
  `noCollision()` like vanilla crops — no more invisible solid blocks over the beds. (26.x renamed the
  builder from the old `noCollission`, which is why the original registrations lacked it.)
- **Gallery reworked**: the fluid blocks are excluded from the block grid (they flooded the display),
  and the exhibits are richer — the numbered fabrication chain (extract → infuse/ladder → **fusion
  demo** (Iron Fragments + Redstone → Nero Alloy Seed) → synthesize (fully staged) → research), a
  **Prospora farmland plot**, the Fertiliser Applicator, and a working farm whose beds are seeded for
  the hopping-pot loop, flanked by Planter + Harvester with their **area holograms switched on**.

### Playtest round 4 fixes (2026-07-16)

- **All machines now accept battery/pipe power.** Root cause of "machines not pulling power": only the
  fabrication machines and grow beds registered an energy capability, so Core's battery push (which
  pushes into adjacent receivers every tick) couldn't see the other nine machines. Every machine block
  entity now exposes **energy + fluid** (through the gated side-config views) and **items** (when it is
  a container) on all three loaders — batteries, hoppers and pipes reach the Genetics Station, Planter/
  Harvester/Applicator, Crop Tower, Greenhouse, Bioreactor, Biofuel Converter, Fertiliser Processor,
  Pollination Beacon and Terraforming Controller.

- **Every material's fragments now convert into their resource.** Conversion previously required a
  per-material recipe and only five existed (coal, iron, diamond, nether star, echo shard) — all other
  materials' Resource Fragments were dead ends. A **generic conversion recipe** now backs every catalog
  material: output and fragment cost come from the material definition (per-material recipe JSONs still
  override when present).
- **All UI gauges are labelled.** A shared labelled-bar style (caption drawn inside a taller bar) now
  identifies every bar in every screen: **Energy** (amber, NeroFlux), **Nutrient** (teal),
  **Progress/Splicing** (green) — across the fabrication machines, Genetics Station, Crop Tower, Grow
  Bed, Planter/Harvester/Applicator, processors, and the status panels. Layouts were re-banded so
  labels, slots and text never overlap (including the Fertiliser Applicator overlap from the
  screenshot: upgrades moved to the right edge, the middle band stacks mode / range / Show-area /
  Energy on separate lines).

### Fluid rendering on Forge and Fabric (2026-07-25)

- **Fluid models are now registered on all three loaders.** Nutrient and biofuel previously rendered
  as the missing-texture checkerboard on Forge and Fabric, because only NeoForge had the 26.x
  `FluidModel` registration. Forge now bakes and registers the models from
  `ModelEvent.BakeFluidModels`; Fabric registers the unbaked models through Fabric API's
  `FluidRenderingRegistry`. All three build the same still/flow materials
  (`block/<fluid>_still`, `block/<fluid>_flow`, no overlay, no tint), and the chunk render layer is
  derived from sprite transparency by vanilla, so no per-block layer registration is needed.
- **Biofuel is a fluid in its own right.** Both fluids were being built from one shared set of
  properties keyed to nutrient, so biofuel reported nutrient's source/flowing pair, handed out the
  nutrient bucket, and placed a nutrient liquid block — a biofuel source became nutrient on contact
  with the world, which no amount of model registration would have fixed. A new common `FluidKind`
  enum (`NUTRIENT`, `BIOFUEL`) carries each fluid's registry id, density, viscosity, bucket and
  liquid block; `FluidFactory.createSource`/`createFlowing` take the kind, and every loader now
  builds one fluid type and one property set **per fluid**. Biofuel is also lighter and runnier than
  nutrient (density 900 / viscosity 1000 against 1050 / 1100) rather than identical to it.
- Fabric's `NutrientFluid` became `ModFlowingFluid`, parameterised by `FluidKind`, since it backs
  both fluids. The per-loader property sets are built lazily on first use so they can never capture
  a registry entry before `ModFluids` has finished initialising.

## [0.0.1-alpha.2] - 2026-07-13

First consolidated alpha of NeroAgriculture — a sci-fi farming mod built on **Neroland Core**,
targeting **MC 26.1.2 and 26.2** on **Fabric, Forge and NeoForge** (the "six cells"). This release
brings the mod from an empty multiloader skeleton to a feature-complete alpha spanning the full
grow-to-fabricate loop, greenhouses, automation, genetics, life support, terraforming, and a cohesive
hand-authored art set.

### Added

**Material catalog and fabrication chain**

- Data-driven **material catalog** with conservative built-in defaults, a discovery scan cap, and
  datapack overrides/blacklist by material id. Server-authoritative, synced to clients.
- The **Essence Extractor**, **Essence Infuser**, **Seed Synthesizer** and **Seed Research Bench**,
  plus material/charged/blank seeds, the five neutral **essences** and **Material Essence**, with
  extracting / infusing / synthesizing / researching / conversion recipes.
- Per-tier **yield curves and yield caps**, progression-gate enforcement at start and completion,
  and an operator diagnostics command (`/neroagriculture catalog list|show|errors|report`).

**Resource crops and grow beds**

- **Resource crops** with eight growth stages, grown on tier **grow beds** (Terran, Industrial,
  Orbital, Colonial, Deepvoid). Right-click a fully grown crop to harvest its essence — the crop
  stays planted.
- Grow beds consume energy + nutrient per growth by tier; Terran is ungated and free.

**Food, alien crops and genetics**

- **Food** and **alien** species crops, food seeds, **Engineered Food** and **Alien Produce**, food
  effects and hybrid signature effects, and synthesizer gating for alien acquisition.
- **Genetics** — deterministic traits, breeding, mutations and pollination via the **Genetics
  Station** and **Pollination Beacon**.

**Greenhouses and growth conditions**

- Sealed **greenhouse** multiblock with cached validation, a dimension-based **environment model**,
  climate/growth-condition evaluation, and per-crop blocked-reason status.

**Automation, fertiliser and life support**

- **Planter**, **Harvester** and **Fertiliser Applicator** area machines with POPIA-safe opt-out
  owner tracking; **Fertiliser Processor** plus speed/yield fertilisers and per-bed dosing.
- **Oxygen Plant / Bioreactor** closed-loop life support with an oxygen-contribution seam.

**Biofuel, essence blocks and crop towers**

- **Biofuel** fluid + **Biofuel Converter** (renewable, below-primary-generation baseload) with a
  provider seam and `c:` energy/fuel tags.
- Compacted **essence blocks** for all five tiers, the finite **Essence Decor** block, and 9↔1
  compression recipes.
- **Crop Tower** controller + frame with virtual-slot batched plant/grow/harvest cycles.

**Seasonal cycles, terraforming and compatibility**

- Datapack-driven **seasonal/stellar cycles** applying growth and yield modifiers.
- **Terraforming** — staged region conversion via the Terraforming Controller + Terraforming Seed,
  with an environment region-override seam.
- **Compatibility** facade (`CompatContracts`) and `c:` discovery tags — every third-party
  integration is dormant-safe when the other mod is absent; NeroAgriculture hard-depends on Core only.

**Visuals and UI**

- A cohesive **"hydroponics lab"** texture set for every block and item (bright composite housing +
  bio-green accent + soil/foliage organics), generated procedurally by `tools/gen_textures.py`; the
  five essence tiers are coloured from the material catalog so art stays in lockstep with data.
- **Eight distinct growth sprites** per crop family (resource / food / alien) as cross models.
- **Layered multi-element 3D block models** for machines, grow beds and the beacon (plinth + inset
  body + console + posts; walled soil trays; column + lens), matching NeroTech/NeroSpace.
- **Per-machine GUI screens** dispatched by machine kind (Extractor / Infuser / Synthesizer /
  Research Bench), each with its own accent and input→process→output diagram.
- **`/neroagriculture gallery`** creative showcase command with a working demonstration **farm**
  (powered + nutrient-fed tier beds growing representative ores) and `gallery clear`.

**Telemetry, docs and tooling**

- Opt-out, anonymous **Sentry** crash reporting (EU ingest) — NeroAgriculture-only filtering, PII
  scrubbing, de-duplication and a per-session cap; toggle with `telemetryEnabled` in
  `config/neroagriculture.properties`. Adds a `PlatformInfo` ServiceLoader seam.
- Full player- and contributor-facing **wiki** under `wiki/`, plus a **wiki sync** GitHub workflow
  that publishes it to the repository wiki.

### Fixed

- **Own creative tab** — NeroAgriculture's items now live in their own creative tab instead of
  Neroland Core's.
- **Block render occlusion** — non-full-cube machines, beds and the beacon are now `noOcclusion`, and
  greenhouse glass uses a dedicated block that culls shared faces against adjacent glass (fixing the
  transparent-model-on-solid-block artefact).

### Notes

- **Privacy (POPIA/GDPR):** all player data is UUID-only, opt-out, and routed through Core's shared
  `PlayerDataErasure` hook; telemetry carries no personal data. See
  [Privacy and erasure](wiki/Privacy-and-Erasure.md).
- **Build:** all six loader×MC cells compile and pass `ecjCheck` and unit tests. Full in-game runtime
  verification across every cell is tracked for a later pass.
