# Changelog

All notable changes to **NeroAgriculture** are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0-beta.1] - 2026-07-30

The **resource-seed economy** release. The mod is reframed around a standalone fragment ladder with
NeroAgriculture-native progression: the word "essence" is gone, every machine reports through a real
screen, and a full code + design audit was remediated in the same
cycle. Builds green across all six loader×MC cells (Fabric/Forge/NeoForge × 26.1.2/26.2).

### Added

**The fragment ladder and resource seeds**

- **Cosmic-ascent fragment ladder** — five tier items **Territe → Forgite → Orbite → Colonite →
  Voidite Fragment** (replacing the essences), condensed 4:1 up the ladder on the **Fragment
  Infuser** with steeply scaling NeroFlux + time costs. The per-resource drop is the **Resource
  Fragment**; compacted fragment blocks and decor follow the same naming.
- **Native, standalone progression** — NeroAgriculture-owned datapack gates `refinement` /
  `synthesis` / `transmutation` / `ascension` that open themselves as each tier's fragments are first
  produced, so the whole ladder is reachable with only Neroland Core installed.
- **Resource seeds require the real resource**: synthesized from the resource + tier-scaled matching
  fragments + a **Prospora Seed** — seeds amplify what you have found, never mint what you haven't.
  Harvests have a configurable seed-return chance (`growth.seed_return_percent`, default 10%).
- **Prospora base crop** — a farmland-planted bootstrap crop dropping Territe Fragments + its seed.
- **Fusion / alloy seeds** — data-driven `fragment_fusion` recipes on the Infuser; curated Steel,
  Energized Steel (dormant-safe) and Core-native Nero Alloy, fully datapack-extendable and clamped by
  `fusion.enabled` / `fusion.max_tier`.
- **Generic fragment → resource conversion** for every catalog material (per-material recipe JSONs
  still override), and a **dynamic creative tab** listing one seed per synced catalog material.
- **Tag-driven discovery** across `c:ingots/gems/ores/dusts/raw_materials`, with per-resource
  tier/gate/yield/conversion/colour/enabled overrides and a configurable default tier for unknown
  materials (`discovery.default_tier`, default Orbite).
- **Resource-colour tinting** — seeds, fragments and planted crops tint to their resource's ingot
  colour on all three loaders, driven by the shared colour resolver.

**Machines, screens and UX**

- **Every machine now has a real screen** in the shared texture-free style: fabrication machines,
  Genetics Station, Crop Tower, Grow Beds, Planter/Harvester/Fertiliser Applicator, the processors,
  and slot-free status panels for Greenhouse / Terraforming / Pollination Beacon. All gauges are
  labelled (Energy / Nutrient / Progress), and gauge values sync as a fraction of the live configured
  capacity.
- **Growth progress readouts** — Grow Beds show a live growth bar for the planted crop; Crop Towers
  show average growth plus a "Ripe n/m" mature-slot counter.
- **In-GUI build guides** — the Crop Tower and Greenhouse Controller screens carry a scrollable
  left-hand guide (with a block-layer sketch) walking through the build steps.
- **Working-area hologram toggle** on the area machines (end-rod outline, persisted per machine).
- **Grow beds are hopping pots** — auto-harvest into four hopper-extractable output slots and
  auto-replant from the seed slot; the full farm loop automates on the bed.
- **Greenhouse Door** — a two-block metal-and-glass airlock door: it counts as sealed shell whether
  open or closed, so walking in never breaches the greenhouse. Crafted 2×3 from Greenhouse Glass over
  Greenhouse Frame (yields 3).

**Ecosystem integration**

- **Nerospace planet-visit adapter** (runtime-guarded, no hard dependency): planet visits grant Core
  `material_discovered` milestones for planet-bound materials, making Nerospace-restricted materials
  researchable; historical visits backfill when Nerospace ≥ 1.0.0-beta.8 is present. Toggle:
  `compat.nerospace_visits`.
- **Optional sibling overlays** (`progression.sibling_overlays`, default off): higher tiers can
  additionally require the matching Neroland arc gate a sibling mod drives open — dormant-safe,
  standalone play is never blocked.

### Changed

- **Progression posture (playtest decision, 2026-07-16):** hard tier gates are **off by default** —
  no machine step or material is gate-blocked out of the box; the native gates stay registered for
  packs that want unlocks back. Pacing comes from the economy (real-resource sacrifice, the 4:1
  ladder, NF + time, yield caps) plus **Seed Research, now required by default**
  (`progression.require_research=true`).
- **Gate unlocks and machine/crop/pollination checks are owner-bound** (placer-recorded,
  erasure-covered), with a nearest-player fallback only for ownerless blocks.
- **Crop towers respect the environment**: per-layer climate checks with a configurable NeroFlux
  surcharge in hostile environments (`crop_tower.hostile_environment_nf_multiplier`, default 4)
  instead of bypassing the system.
- **Area machines** (Planter/Harvester/Applicator) start at a 7×7 area, extendable to 13×13 with
  range upgrades.
- **Biofuel is a fluid in its own right** — own properties, bucket and liquid block (lighter and
  runnier than nutrient) instead of sharing nutrient's definition.
- **Food & alien content de-emphasised** to a clearly-labelled secondary system; nothing removed.
- **Texture set repainted** to the fragment naming with a ~15% darker lab palette; tinted items use
  neutral greyscale bases so the resource colour multiplies cleanly.
- Internal: canonical cross-loader machine and screen tables in `common` (new machines can no longer
  silently miss capabilities or screens on one loader).

### Fixed

**Audit remediation (2026-07-29)**

- **Recipe lookup no longer scans and sorts the whole recipe manager every tick** — per-type cached
  index plus an idle gate on the fabrication machines; material matching cached per catalog.
- **Breaking a Grow Bed or Foundation Machine no longer voids its inventory**; all machines drop
  contents from the removal hook, covering creative breaks and explosions.
- **Crop block entities sync to the client** — in-world crop tints render the real material colour;
  grow beds no longer broadcast a full sync packet on every dirty-mark.
- **Server-scoped static caches are cleared on server stop** (terraforming regions, greenhouse index,
  cycles, catalog, gallery records, visit tracking) instead of leaking into the next world.
- **Forge honours per-side machine side-config dynamically**, matching Fabric/NeoForge.
- **Platform services preload at construction** even with telemetry opted out (mid-tick ServiceLoader
  crash class).
- **Player-data erasure reaches machines in unloaded chunks** via a persisted tombstone set with a
  retention window (`privacy.erasure_retention_days`, default 90). (POPIA/GDPR)
- **Greenhouse oxygen contributions are retracted** on breach/unpower/removal.
- Plus a full medium/low sweep: synthesizer hopper filter matches its recipe; tower harvests no
  longer burn fertiliser on jammed outputs and split yields across stacks; dedicated-server clients
  resolve materials/species from the synced catalog; screens no longer hard-code gauge maxima;
  compatibility panels cached per screen; menu slot filters, parser hardening, unified colour
  parsing, aggregated reload logging, surgical `gallery clear`, network action validation, Fabric
  dedicated-server classloading, zero-yield age reset, and client cache clearing on disconnect.

**Playtest and rendering fixes**

- **All machines accept battery/pipe power** — every machine exposes energy + fluid (side-config
  gated) and items on all three loaders.
- **Fluids render on all three loaders** (animated still/flow sprites; previously missing-texture
  checkerboard outside NeoForge).
- **UI overlap sweep** — fabrication, area-machine, grow-bed, genetics and tower screens re-banded so
  labels, slots and text never collide; the Crop Tower screen gained dedicated rows for its gauges,
  growth bar and status line.
- **Right-click with an item in hand opens machine UIs** instead of placing the held block; crops are
  walk-through (`noCollision`); block items translate via their block keys; seeds and fragments are
  named for their resource ("Iron Seed", "Redstone Fragment").
- **Gallery**: exhibits record what they place so `clear` removes exactly that; fluids and doors are
  excluded from the floating grid; the greenhouse exhibit's battery no longer blocks the controller's
  face and the dome has a walk-in Greenhouse Door.

### Notes

- **Privacy (POPIA/GDPR):** player data is UUID-only, opt-out, and routed through Core's shared
  `PlayerDataErasure` hook — now including unloaded-chunk machine owners via erasure tombstones;
  telemetry carries no personal data. See [Privacy and erasure](wiki/Privacy-and-Erasure.md).
- **Build:** all six cells compile and pass `ecjCheck` and unit tests. The new screens, door and
  tint paths are compile-verified; in-game visual checks continue on the dev machine.

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
