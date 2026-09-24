# Recipe Viewers (JEI & EMI)

NeroAgriculture's fabrication recipes show up in both **JEI** and **EMI**. Both are optional: the mod
runs the same without either.

## Pages

One page per fabrication recipe type, with the machines that run it as the page's workstations:

| Page | Recipe type | Workstation | What the page shows |
| --- | --- | --- | --- |
| Material Extraction | `neroagriculture:material_extraction` | Fragment Extractor | Ore in; Tier Fragments plus the material's Resource Fragment out |
| Fragment Infusing | `neroagriculture:fragment_infusing` | Fragment Infuser | Tier Fragments in (and a Blank Seed when charging); upgraded fragment or Charged Seed out |
| Fragment Fusion | `neroagriculture:fragment_fusion` | Fragment Infuser | A material's Resource Fragment plus the second input; the alloy's Resource Seed out |
| Seed Synthesizing | `neroagriculture:seed_synthesizing` | Seed Synthesizer | The real resource, the matching Tier Fragments and a Prospora Seed; the Resource Seed out |
| Material Conversion | `neroagriculture:material_conversion` | Seed Synthesizer | A material's Resource Fragments; the finished resource out. The count is the recipe's minimum; the material catalog can require more |
| Seed Research | `neroagriculture:seed_researching` | Seed Research Bench | The sample in. Food and alien research gives the species seed; resource research only unlocks that Resource Seed for synthesis, so its seed is shown but never offered as a way to make it |

Each page also lists the recipe's energy and time before machine upgrades (research is instant).
The catalog's material-less conversion fallback has no entry, because what it produces depends on the
server's catalog. Resource Fragments and Seeds
carry their material, so they read as "Iron Fragment", "Steel Seed" and so on. The material's tier comes
from the recipe or, failing that, from the material catalog the server sends on join; if neither knows
it, the plain item is shown.

The pages show what a recipe can produce, not what a given player can make right now. Gates, research
requirements and the fusion tier cap still apply in the machine.

## Datapack recipes

All six types are datapack-driven (see [Material recipe datapacks](Material-Recipe-Datapacks.md)), so a
pack's added or overridden recipes appear on these pages too. Since Minecraft 26.x the client no longer
receives the full recipe list, so NeroAgriculture opts its fabrication recipes into the server's recipe
sync on NeoForge and Fabric. A server running an older NeroAgriculture that does not send them leaves the
pages empty.

## EMI on Minecraft 26.x

Official EMI has no Minecraft 26.x release yet. EMI support is built against the community **EMI
Unofficial Port (Unstable)** on CurseForge, which keeps EMI's normal plugin API:

- **NeoForge and Fabric:** supported on 26.1.2, 26.2 and 26.3.
- **Forge:** neither JEI nor the EMI port ships a Forge runtime for 26.x, so there are no pages on Forge.

With both JEI and EMI installed, EMI takes over the recipe screens and shows these pages once, from
NeroAgriculture's own EMI plugin rather than through EMI's JEI bridge.

## For contributors

- Shared layout: `common/.../compat/viewer/` (`FabricationPage`, `FabricationDisplay`, `ViewerRecipes`)
  imports no recipe viewer. `compat/jei/` and `compat/emi/` adapt it; the EMI classes never touch a JEI
  class, so an EMI-only install loads cleanly.
- EMI pins are the port's CurseForge file ids (`emi_file_<loader>_<mc>` in `gradle.properties`), resolved
  through CurseMaven. Dev clients load JEI by default; add `-PwithEmi` to a `runClient` to load EMI too.
