# Sibling-mod progression overlays

NeroAgriculture progresses on its **own native gates** and never needs another mod. Optionally, when a
sibling mod is present, a higher tier can *additionally* require that mod's Neroland arc gate — giving
modpacks a cross-mod "link" feel.

| Tier / gate | Also requires (arc gate) | Opener mod |
| ----------- | ------------------------ | ---------- |
| Forgite / Refinement | `industrial_power` | Nerotech |
| Orbite / Synthesis | `reached_orbit` | Nerospace |
| Colonite / Transmutation | `first_colony` | NeroColonies |
| Voidite / Ascension | `deep_space` | Nerospace |

## Config: `progression.sibling_overlays`

- **`auto`** (default) — an overlay applies **only when the opener mod is loaded**. A Core-only game is
  never gated by an arc gate nothing could open, so **standalone play is never blocked**.
- **`on`** — always require the arc gate (for packs that drive it by other means).
- **`off`** — only the native gates apply.

The overlay is layered onto every resource-tier check — machines, planting, growth, harvest,
automation and towers — and is dormant-safe when the sibling is absent.
