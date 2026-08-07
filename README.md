# NeroAgriculture

> Part of the Neroland sci-fi Minecraft mod ecosystem, built on **Neroland Core**.

**Status:** `0.1.0-beta.1` (tag `v0.1.0-beta.1`, 2026-07-30) — the **resource-seed economy**
release. Progression runs on the standalone Cosmic-ascent fragment ladder — Territe → Forgite →
Orbite → Colonite → Voidite — with the Fragment Extractor, Fragment Infuser, Seed Synthesizer and
Seed Research Bench driving the datapack-driven resource-fragment conversion chain, plus grow beds,
greenhouses, crop towers, genetics, automation and terraforming.

## Build targets

- **Minecraft:** 26.1.2 and 26.2
- **Loaders:** NeoForge, MinecraftForge/Forge, Fabric (the "6 cells")
- **Java:** 25
- Mod id: `neroagriculture` · package `za.co.neroland.neroagriculture`

## Layout

The build is the repo root, with a flattened cross-loader structure driven by Stonecutter:

- `common/` — shared, loader-agnostic source spliced into every loader node
- `fabric/` — Fabric Loom
- `forge/` — ForgeGradle
- `neoforge/` — ModDevGradle
- `stonecutter.gradle` — the real root build script; `build.gradle` is intentionally inert

## Building

Neroland Core `1.8.0` is a required dependency. Local builds prefer artifacts published by the
sibling `../neroland-core` checkout; CI and fresh clones use GitHub Packages. See
[`USING-CORE.md`](USING-CORE.md) for setup and authentication details.

```sh
./gradlew :fabric:26.2:build          # one cell
./gradlew :neoforge:26.1.2:build :neoforge:26.2:build \
          :forge:26.1.2:build :forge:26.2:build \
          :fabric:26.1.2:build :fabric:26.2:build   # all six
```

## Telemetry and privacy

NeroAgriculture ships **opt-out** anonymous crash reporting via Sentry (EU-region servers) so bugs in
the mod can be found and fixed. Reports carry a stack trace plus version strings (mod / Minecraft /
loader / OS / Java) and the list of loaded mod ids — never names, UUIDs, IPs or world data. Opt out
by setting `telemetryEnabled = false` in `config/neroagriculture.properties`.

Full disclosure, including what player data the mod stores and how erasure works:
[`PRIVACY.md`](PRIVACY.md).

See [`AGENTS.md`](AGENTS.md) / [`CLAUDE.md`](CLAUDE.md) for agent and contributor context.
