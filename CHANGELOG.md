# Changelog

All notable changes to **NeroAgriculture** are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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

> Follow-ons still open in this area: the optional Prospora **base crop** (planting Prospora → Territe
> Fragments; Territe is already obtainable via tier-1 extraction, so nothing is blocked) and explicit
> seed-return-chance tuning on the harvest→resource loop.

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
