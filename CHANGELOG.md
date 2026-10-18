# Changelog

All notable changes to **NeroAgriculture** are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

Companion-client support, a two-way Nerospace environment bridge and two POPIA/GDPR fixes. No block,
item, recipe or tag id changes, and no gameplay balance change.

### Added

**Nerospace environment and oxygen bridge**

- On Nerospace planets, Nerospace now answers the open-air growing environment directly — atmosphere,
  hazard and terraforming stage — instead of NeroAgriculture guessing from its own dimension table. The
  practical effect is that a **terraformed** planet supports open-sky farming while a dead one still
  needs a sealed greenhouse.
- A formed, powered greenhouse publishes its oxygen production back into Nerospace's atmosphere model
  as a bounded, expiring contribution around the controller. Breaching, unpowering or breaking the
  greenhouse retracts it at once.
- Nerospace stays silent for the overworld, the nether, the end and every other mod's dimension, so
  datapack environments are never overridden. A completed local terraforming project still wins over
  Nerospace's reading of the same spot.
- New config key `compat.nerospace_environment` (default `true`, server-authoritative).
- Nerospace is consumed as an **optional compile-only** dependency (`nerospace_version`, new, pinned
  to `1.1.0` — the first Nerospace release published for all nine loader x MC cells; the API surface
  used has been stable since `1.0.3`). It is listed as optional in all three manifests, is neither
  shipped nor required, and every Nerospace type sits behind a holder class the JVM only loads once
  the mod is confirmed present — so the mod runs unchanged with Nerospace absent. Nothing reflects.
- **Not wired, deliberately:** `CycleApi` (Nerospace exposes no public space-weather query) and the
  terraforming overlay hand-off (Nerospace's overlay API is claim-gated and needs an acting player, so
  forwarding our block-entity events would be a silent no-op on every server).
- **Privacy:** nothing player-identifying crosses this seam in either direction. The environment query
  carries no actor, and the oxygen source id is derived from the greenhouse controller's block
  position, never from its owner.

**NeroLink module** (`za.co.neroland.neroagriculture.link`, module id `neroagriculture`, schema `1`)

- NeroAgriculture now registers with Neroland Core's link registry, so a companion client can show you
  your own farm. Four read sections — `crops` (your grow beds, what is planted and what is blocking
  it), `machines` (fabrication, area and terraforming machines with progress), `greenhouse` (the
  greenhouses enclosing your beds: seal state, volume, oxygen, leak) and `fragments` (per-tier progress
  up the Cosmic-ascent ladder, discovered materials and gate state).
- Two owner-scoped live topics — `greenhouse` (a greenhouse you farm in changed seal state) and
  `terraforming` (one of your projects reached a new stage) — plus one rate-limited alert when a
  greenhouse you farm in stops being sealed. No broadcast topics: a farm is not a public place.
- **One action, `acknowledge_alert`, and nothing else.** Every other candidate write creates items,
  consumes items or changes the world, which is the class of action that should require being in the
  world. No link action can plant, harvest, fertilise or alter crops.
- **No `towers` section.** The Crop Tower Controller records no owner and its in-world gate check falls
  back to `getNearestPlayer`, which is not a permission model — a player idling near somebody else's
  tower would have been handed its contents. The section is omitted rather than approximated, and
  becomes available (additively) once the controller records a placing player. `greenhouse` passes the
  same test only because it is scoped indirectly: a greenhouse is reported to you when it encloses a
  grow bed *you* own, so the authorisation comes from the bed.
- New config key `link.module_enabled` (default `true`); `false` registers nothing at all. A failure
  registering the module is logged and swallowed — farming is unaffected.
- New wiki page [Link module](wiki/Link-Module.md), and the new surface is disclosed in
  [`PRIVACY.md`](PRIVACY.md) and [Privacy and erasure](wiki/Privacy-and-Erasure.md).

### Fixed

- **Machines, grow beds, controllers and fragment blocks drop again when mined with a pickaxe.**
  The mod shipped no `minecraft:mineable/pickaxe` block tag, so every block that needs the correct
  tool (grow beds, all machines and controllers) dropped nothing when broken, and the frames, glass,
  door and fragment blocks mined slowly. They are now all tagged as pickaxe-mineable (any pickaxe
  tier). Crops and fluids are unchanged.
- **Block and loot drops work as intended on Minecraft 26.3** (NeoForge, Fabric and Forge). Minecraft
  26.3 changed the loot-table file format and silently ignores the old one, so conditions and drop
  modifiers (explosion survival, door/slab block-state checks, drop counts, random chances) were skipped.
  The 26.3 builds now ship every loot table in the new format. 26.1.2 and 26.2 were not affected.
- **The erasure tombstone store no longer crashes the server on a corrupt file.**
  `ErasedOwners.get` read `data/erased_owners.dat` straight through `computeIfAbsent`, and it is
  consulted on *every owned block-entity load* — so an unreadable file threw once per chunk load,
  forever, **and** stopped erased owners being dropped from saved NBT. That made it an erasure failure,
  not only a stability bug. Every fetch now routes through Neroland Core's shared
  `za.co.neroland.nerolandcore.data.SavedDataRecovery` (Core 1.15.0, the version already required),
  with the primary → last-known-good backup → fresh-store ladder used across the ecosystem, reporting
  the failure as handled telemetry instead of crashing.
- **Erasure and retention now reach the recovery backup.** The backup is a second copy of the same
  UUIDs, so a new tombstone is forced into it in the same request that records the erasure, and an
  expired tombstone is forced out of it in the same pass that prunes it — the
  `privacy.erasure_retention_days` window applies to both copies.
- **The Nerospace planet-visit adapter's session map is registered for erasure.** The in-memory
  last-seen-dimension entry was session-scoped and therefore low risk, but an erasure request now
  clears it immediately rather than waiting for the player to disconnect.

### Changed

- **The Nerospace visit-history backfill now calls Nerospace's public API directly.** On join,
  historical planet visits are read from `NerospaceVisits.visitedPlanets` (Nerospace 1.0.3+) through
  the same optional compile-only dependency as the environment bridge, replacing the reflective
  `compat/nerospace/NerospaceVisitHistory` probe that guessed at method names. Behaviour is unchanged
  — visits made before NeroAgriculture was installed are still backfilled once per join, and live
  tracking still compares dimension ids — and any failure still degrades silently to live tracking.
  The `compat.nerospace_visits` description now names Nerospace 1.0.3+ instead of `1.0.0-beta.8`.

## [0.5.0-beta.1] - 2026-10-11

Survival crafting recipes for every machine, controller, grow bed and farm block, a NeroQuests quest
pack, four personal milestone gates and a terraforming channel. Requires Neroland Core 1.15.0; still no
other dependency (NeroQuests stays optional).

### Added

- **Survival crafting recipes** for the whole block line, which until now was creative-only: the
  Fragment Extractor (iron, copper, a furnace, a piston and redstone — vanilla only, so Territe
  extraction is reachable from a fresh world) and the Territe Grow Bed (iron, a composter, dirt); the
  Fragment Infuser, Seed Synthesizer, Seed Research Bench, Planter, Harvester, Fertiliser Applicator,
  Fertiliser Processor, Greenhouse Frame (x8), Greenhouse Glass (x8), Greenhouse Controller, Crop Tower
  Frame (x4), Crop Tower Controller, Speed Module and Efficiency Module, all costing Territe Fragments
  plus vanilla parts; the Genetics Station, Pollination Beacon, Oxygen Plant and Biofuel Converter at
  Forgite; the Terraforming Controller and Terraforming Seed at Orbite; each higher grow bed from the
  bed below it plus four fragments of its tier; and a Blank Seed from wheat seeds, paper and a Territe
  Fragment. Recipes carry no progression gate: the fragment ladder itself paces them. See the wiki's
  Crafting Recipes page.
- **NeroQuests quest pack** — chapter `neroagriculture:field_ledger` "Field Ledger", 15 quests — that
  appears in the quest book when NeroQuests is installed; inert otherwise, no dependency. Five
  vanilla-ingredient crafts (fertilisers, biomass, crop waste) lead to the `ledger_opened` capstone,
  four gate quests follow the tier ladder (Refinement to Ascension), four record personal milestones
  (`first_reaping`, `seed_scholar`, `under_glass`, `vertical_farm`) and one server-wide world-log quest
  (`green_world`) completes when a terraforming project in any dimension reaches Complete.
- **Record gates** `neroagriculture:first_research`, `first_harvest`, `greenhouse_keeper` and
  `tower_grower`: player-scope Neroland Core gates the mod opens once for a player when they complete a
  Seed Research Bench research, harvest a mature NeroAgriculture crop by hand, open a running
  Greenhouse Controller or open a formed Crop Tower Controller. They have no prerequisites and gate
  nothing; the tier gates (`refinement` to `ascension`) are unchanged and keep their prerequisites. The
  greenhouse and tower milestones are recorded on interaction, so neither controller stores an owner.
- **Threshold event `neroagriculture:terraform_stage`** on Neroland Core's `ThresholdEvents` bus,
  fired on every terraforming stage change. Scope = the dimension id (never the position, the project
  id or a player); value and threshold = the stage (Seeded 1, Growing 2, Stabilising 3, Complete 4);
  `rising` is `false` only for a rollback to Dormant (0). The in-process `AgricultureApi` terraforming
  event is unchanged.

### Changed

- **Requires Neroland Core 1.15.0** (was 1.13.0): the `ThresholdEvents.fire(sourceModId, crossing)`
  overload. `neroagriculture:tower_output` crossings now name `neroagriculture` as their source too.
- The four tier quests in the quest pack are visible from the start (they were hidden behind their own
  gate while the ladder had no survival recipes).

## [0.4.0-beta.1] - 2026-10-11

Item tags for tag-keyed pricing, and a crop-tower output threshold event. Neroland Core only: no new
dependency (NeroEconomy is not needed for any of this).

### Added

- **Item tags** for data packs and economy mods to key on, so a tag-keyed commodity follows every new
  item with no code: `#neroagriculture:fragments` (the five tier fragments; the component-keyed
  `resource_fragment` stays out because its identity is in item components),
  `#neroagriculture:fertilisers` (fertiliser, speed and yield fertiliser) and `#neroagriculture:biofuels`
  (biofuel canister and bucket). The fragments and biofuels already sit in `c:fragments` and `c:biofuels`.
- **Threshold event `neroagriculture:tower_output`** on Neroland Core's `ThresholdEvents` bus. Scope = the
  dimension id (never a player); value = items harvested by every crop tower in that dimension in the
  last in-game hour (1,000 game ticks of server game time, a rolling window in ten 100-tick steps);
  threshold = new config `crop_tower.output_event_threshold` (default 512, 0 = off); `rising` is `true`
  once when the total reaches the threshold and `false` once when it drops back below. In-memory only, so
  a restart announces nothing. Usable by NeroQuests' `custom_event` and NeroEvents.

### Deprecated

- `AgricultureApi.PremiumGoodsProvider`, `AgricultureApi.PremiumGood`, `AgricultureApi.PREMIUM_GOODS` and
  `CompatContracts.registerPremiumGoodsProvider`. They were never implemented or read, priced in energy
  rather than credits, and keyed on a player. Price NeroAgriculture goods through the item tags above
  instead. Kept so existing callers still compile.

## [0.3.0-beta.1] - 2026-09-24

JEI and EMI recipe pages for the fabrication machines.

### Added

- **JEI and EMI pages for all six fabrication recipe types:** Material Extraction, Fragment Infusing,
  Fragment Fusion, Seed Synthesizing, Material Conversion and Seed Research, with the Fragment
  Extractor, Fragment Infuser, Seed Synthesizer and Seed Research Bench as workstations. Each page shows
  what the machine actually takes and gives, including the slots the recipe file does not list (a
  synthesizer's Tier Fragments and Prospora Seed, an infuser's Blank Seed, an extractor's Resource
  Fragment), plus the energy and time before upgrades. Resource Fragments and Seeds carry their
  material, so they read as "Iron Fragment", "Steel Seed" and so on. EMI support targets the community
  EMI Unofficial Port (Unstable), the only EMI build for Minecraft 26.x, on NeoForge and Fabric. With JEI
  and EMI both installed, the pages show once. Both viewers stay optional. See the wiki's Recipe viewers
  page.
- **Recipe sync for the fabrication recipes** on NeoForge and Fabric. 26.x clients no longer receive the
  full recipe list, so the server now sends the six fabrication recipe types to clients for the recipe
  viewers. Recipe definitions only; no player data.

## [0.2.1-beta.1] - 2026-09-24

### Fixed

- **Fabric: the game no longer crashes while loading.** The client registered the Resource Crop's tint
  against Minecraft's block colours during client start-up, before those colours exist, so Fabric
  clients stopped with a `NullPointerException` from `Minecraft.getBlockColors()`. The tint is now
  registered through Fabric API's `BlockColorRegistry`, which holds it until the colours are ready. Crops
  keep the same tint. NeoForge and Forge were not affected.

## [0.2.0-beta.1] - 2026-09-20

Minecraft **26.3** support. No gameplay, id, tag or config change.

### Added

- **Minecraft 26.3** as a new Stonecutter node on every loader — NeoForge `26.3.0.7-beta`,
  Forge `26.3-66.0.2` and Fabric (fabric-api `0.161.0+26.3`, NeoForm `26.3-1`) — built alongside
  26.1.2 and 26.2, so every release now ships **nine** loader × version jars.

### Changed

- VS Code run/debug configurations (`.vscode/launch.json`, `.vscode/tasks.json`) gain the three
  26.3 cells; the "Build all" task now builds all nine.
- CI (`multiloader.yml`, `publish.yml`) builds, attaches and publishes the 26.3 jars.
- Requires **Neroland Core 1.13.0** (was `1.8.0`) — the first Core release with a 26.3
  build. The loader range still derives from the pin (`[${nerolandcore_version},2.0)`).
- JEI pins moved to the newest published builds on each Minecraft version: `29.40.0.101` (26.1.2), `30.35.0.223` (26.2) and `31.3.0.18` (26.3). Compile-time API only — JEI remains a soft dependency and the shipped jar gains no hard requirement.

### 26.3 port notes

- Block classes build their codecs through Core's `BlockCodecs` (26.3 removed block-type codecs); `codec()` is kept without `@Override` so one source compiles on every version.
- 26.3 API differences are handled with Stonecutter blocks: `PoseStack#rotate` (was `mulPose`), the new `Prediction` argument on `drop` / `placeItemBackInInventory`, `setPermanentlyInvulnerable`, and similar renames.
- Crop `playerDestroy` overrides follow 26.3's `ServerLevel` / `ServerPlayer` signature. `PushReaction` constants follow 26.3's renames (`IMMOVEABLE`, `POPPED`).
- Build: the shared `common/` Java source is now preprocessed by Stonecutter for every non-active node (`stonecutterProcessCommon`), so common code can carry `//? if >=26.3 {` blocks, and `common/src/main/resources-<mc>` overlay folders are merged over the shared resources for matching nodes (`mergeCommonResources`). The active node still compiles the raw `common/` folder.
- Build plugins aligned with Neroland Core: ModDevGradle `2.0.147` (the older 2.0.141 cannot set up NeoForge 26.3), ForgeGradle `7.0.40`, Stonecutter `0.9.8`.

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
