# NeroAgriculture

**Grow it anywhere — hydroponics, alien crops, greenhouses and terraforming that turn a dead planet green in the Neroland universe.**

NeroAgriculture is the **futuristic farming & terraforming** mod of the Neroland ecosystem — the system that makes a colony self-sufficient and a hostile world liveable. Crops become infrastructure here: they feed populations, breathe oxygen into sealed habitats, and burn as biofuel to power the grid. Farm off-world where there is no soil, no air and no sun unless you build it, engineer your crops with genetics, and terraform whole regions as a long, late-game project.

Built on **Neroland Core**, so its power/upgrade framework, progression gates, config and `c:` material tags are shared with the rest of the lineup. *(Planned — in design; not yet released.)*

---

## Farming as infrastructure

1. **Hydroponics.** Soil-free growing blocks and multiblocks supply water and nutrients directly, letting crops grow where there is no dirt — the foundation of off-world farming, running on Core's power framework.
2. **Greenhouse blocks.** Sealed, lit, pressurised enclosures create an Earth-like pocket on hostile worlds, satisfying atmosphere and light requirements artificially for a power cost.
3. **Growth conditions.** Every crop checks its environment — light, water/nutrient, atmosphere, oxygen, temperature and planet tags — and grows only when satisfied, so vanilla dirt-farming simply fails in vacuum.
4. **Fertiliser machines.** Recipe-driven processors turn raw inputs into fertiliser that accelerates growth or boosts yield on nearby crops.
5. **Genetic crop upgrades.** Stations apply persistent trait modifiers — yield, growth speed, hardiness, oxygen output — stored on the seed/crop, so you can engineer your farm over time.
6. **Automated crop towers.** Multiblock vertical farms plant, grow and harvest on a **batched** cycle, exposing item I/O for logistics and storage networks — the automation endgame of farming.
7. **Biofuel production.** Converters turn biomass and crop surplus into a Core-power-tagged fuel/energy output, closing the loop between farming and power.

## Off-world & terraforming

- 🌱 **Alien crops & planet-specific food** — new species with non-Earth growth needs and planet-gated produce that carries planet-themed buffs, giving you a reason to farm each world.
- 🫁 **Oxygen-producing plants** — flora that generate breathable oxygen for a sealed area and double as life support, tied into Nerospace's oxygen/atmosphere system.
- 🌍 **Terraforming seeds** — specialised seeds that, once established, slowly shift a region's local atmosphere or biome toward habitability through Nerospace atmosphere hooks — a slow, area-based arc, never a single click.
- 🎛️ **Configurable realism** — server owners dial how punishing off-world growing is (oxygen/atmosphere strictness), plus growth rates, genetics ceilings, biofuel energy values, and terraforming speed and reversibility.
- 🧵 **Server-friendly** — growth, terraforming and machine state are server-authoritative and persist with the world; terraforming respects claims/territory and harvest cycles are batched to avoid thrashing chunk updates.

## Privacy (POPIA / GDPR)

NeroAgriculture stores **no personal data**. Crop, genetics and terraforming state is world/gameplay data tied to the save, never to a player's identity. Any optional crash telemetry is anonymous and opt-out, carrying **version strings only** — never names, UUIDs, IPs or world data.

## Why it fits the ecosystem

- 🧩 **Built on Neroland Core** — one power/upgrade framework, one progression arc, shared `c:` material and power tags, and its own creative tab.
- 🚀 **Interoperates, never hard-depends** — **Nerospace** is the key partner (planet tags, atmosphere/oxygen rules and biome hooks that the growth and terraforming systems build on), while **NeroColonies** is the main consumer of food, oxygen and produce. **Nerotech**/**NeroPower** can burn biofuel and power your greenhouses, and **NeroLogistics** routes crop-tower output — all optional, all soft.
- 🔌 **External-ready via Core tags** — crop-tower item I/O and biofuel energy interoperate with Create, AE2, Mekanism and Ad Astra planet dimensions through Core's tags as the 26.x ecosystem fills in; **Energized Power** interop (its Plant Growth Chamber and FE machines) is live now.
- 🧱 **Cross-loader** — NeoForge, Forge and Fabric on Minecraft **26.1.2** and **26.2**.

## Requirements & compatibility

- **Requires [Neroland Core](https://modrinth.com/mod/nerolandcore)** — install it alongside NeroAgriculture (it loads first).
- Optional but recommended: **[Nerospace](https://modrinth.com/mod/nerospace)** for the planet, atmosphere and oxygen rules that give off-world farming and terraforming their meaning.
- **Modpacks are allowed and encouraged** — any platform, no need to ask. Use the official files and credit *NeroAgriculture by Neroland* with links to the [CurseForge page](https://www.curseforge.com/minecraft/mc-mods/neroagriculture) and the [GitHub repository](https://github.com/Neroland/neroagriculture). Full terms: [LICENSE](https://github.com/Neroland/neroagriculture/blob/main/LICENSE).

## Links

- 📖 **[Wiki](https://github.com/Neroland/neroagriculture/wiki)** — every crop, machine, and system documented.
- 💬 **[Discord](https://discord.gg/ArPXvYUzJG)** — chat, help, and sneak peeks.
- 🐞 **[Issues](https://github.com/Neroland/neroagriculture/issues)** — bug reports and feature requests.
- 🗒️ **[Changelog](https://github.com/Neroland/neroagriculture/blob/main/CHANGELOG.md)**
- 🔥 **[Also on CurseForge](https://www.curseforge.com/minecraft/mc-mods/neroagriculture)**

---

*Created by Neroland. The project logo was made with the help of AI image tools; in-game art is generated by the project's own tooling and refined by hand.*
