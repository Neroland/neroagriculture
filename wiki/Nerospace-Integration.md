# Nerospace Integration

NeroAgriculture integrates with Nerospace in two independent places:

1. **Planet visits** — unlocking research for planet-bound materials. Live tracking uses no Nerospace
   code at all; the one-off backfill on join reads Nerospace's public visit-history API.
2. **Environment and oxygen** — Nerospace decides what the open air is like on its planets, and your
   greenhouses feed their oxygen back into its atmosphere. Uses Nerospace's public API.

Both are optional, both are individually switchable, and the mod runs unchanged with Nerospace absent.

---

## Planet visits

Some catalog materials are **planet-bound**: their definition carries a dimension restriction in the
`nerospace` namespace (Nerospace planet ores and planet-restricted meteor materials). For these, the
[Seed Research Bench](Seed-Research.md) refuses the usual owner-mod/pickup evidence — the Core
`material_discovered` milestone can only come from **actually visiting the planet**. This page covers the
runtime-guarded adapter that records those visits.

## What it does

- **Live visits** — when a player enters a `nerospace:*` dimension (rocket landing, dimension change, or
  logging in there), the server grants `material_discovered` for every catalog material bound to that
  dimension. From then on the bench researches those materials normally.
- **Historical backfill** — on each join, the adapter asks Nerospace's public visit-history API
  (`za.co.neroland.nerospace.api.NerospaceVisits`, Nerospace `1.0.3` or newer) once for the planets
  that player has visited, and grants milestones for planets visited *before* NeroAgriculture was added
  to the world.

Grants are idempotent — a milestone already observed is never re-written — and event-driven: at most one
cross-mod query per player per login, no polling on NeoForge or Forge, and on Fabric only a five-second
dimension diff (that loader's API ships no server-side dimension-change event).

### Behaviour without Nerospace

| Situation | Behaviour |
| --- | --- |
| Nerospace absent | Fully dormant. Planet dimensions cannot exist, so nothing fires; planet-bound materials stay locked (they are unreachable content anyway). |
| Nerospace present | Live visit tracking works — it uses only dimension identity, no Nerospace code. Historical backfill runs on join; if the visit-history query fails for any reason it is switched off for the session and live tracking carries on. |

---

## Environment and oxygen

On a Nerospace planet, Nerospace is the authority on what the open air is like — so it answers the
growing environment directly instead of NeroAgriculture guessing from its own dimension table.

**What Nerospace tells us.** For a position on one of its planets, its public API reports the
atmosphere, the ambient hazard, the oxygen level and the terraforming stage. That maps onto a growing
environment like this:

| Nerospace reading | Growing environment |
| --- | --- |
| Breathable air | Oxygenated |
| Anything other than hard vacuum | Pressurised |
| Heat hazard (Cindara) | Hot |
| Cold hazard (Glacira) | Cold |
| Terraforming stage 1 or higher | Temperate — the biome has been rewritten and the chunk is breathable |

The practical effect: a dead planet needs a sealed greenhouse, and a **terraformed** one does not. That
is the whole payoff of the late arc — open-sky farming on a world that used to kill you.

**What we tell Nerospace.** A formed, powered greenhouse publishes its oxygen production into
Nerospace's atmosphere model as a bounded contribution around the controller (16 blocks, refreshed
every revalidation, expiring after a minute if it stops). Breaching, unpowering or breaking the
greenhouse retracts it immediately. So a greenhouse makes its doorstep genuinely breathable, and losing
one takes the air with it.

### Behaviour without Nerospace

| Situation | Behaviour |
| --- | --- |
| Nerospace absent | Dormant. The local dimension environment table and datapack layer keep full authority, and greenhouse oxygen stays in the local life-support model. |
| Nerospace present, on a Nerospace planet | Nerospace answers; greenhouse oxygen is published to it. |
| Nerospace present, anywhere else | Silent. Nerospace never answers for the overworld, the nether, the end or another mod's dimension, so your datapack environments are never overridden. |

A **completed local terraforming project** still wins over Nerospace's reading of the same spot, because
NeroAgriculture's own reversible overlay is registered first.

### How the compile-time dependency stays optional

Nerospace publishes its public API as a Maven artifact, so the environment bridge and the visit
backfill consume it as a `compileOnly` dependency rather than through reflection. In each, every
Nerospace type sits inside a nested holder class that the JVM only loads once the mod is confirmed
present, so with Nerospace absent those classes are never linked and nothing fails. Live visit
tracking references no Nerospace type at all.

NeroAgriculture still never *hard*-depends on Nerospace: the manifests list it as optional (`suggests` on
Fabric, `optional` / `mandatory = false` on NeoForge and Forge), and the jar neither ships nor requires
it.

---

## Configuration

| Key | Default | Effect |
| --- | --- | --- |
| `compat.nerospace_visits` | `true` | Record planet visits. When `false`, planet-bound materials cannot earn `material_discovered`. |
| `compat.nerospace_environment` | `true` | Let Nerospace answer the open-air environment on its planets and receive greenhouse oxygen. When `false`, the local environment table and life-support model are used everywhere. |

## Privacy (POPIA/GDPR)

Nothing player-identifying crosses either integration, in either direction. The environment query is a
pure read with no actor, and the oxygen contribution is keyed by the greenhouse controller's block
position — never by its owner.

- **What is read:** the player's current dimension, server-side, at join and on a dimension change;
  and, once per join, that same player's own Nerospace planet-visit list (planet ids only), used solely
  to credit their own research. Nothing is sent to Nerospace.
- **What is stored:** only the resulting `material_discovered` milestone grants, in Neroland Core's
  `MaterialMilestones` store. NeroAgriculture keeps no shadow visit log and no timestamps. The one
  in-memory value — each online player's last-seen dimension, used by the Fabric diff — is session-only,
  dropped on disconnect and on server stop, and additionally cleared immediately by a Core erasure
  request.
- **Erasure:** milestone grants live entirely in Core, so a single `PlayerDataErasure` request purges
  them along with all other research milestones. Logs carry material and dimension ids only — never
  player names or UUIDs.

## See also

- [Seed research](Seed-Research.md) — where the `material_discovered` milestone is consumed
- [Sibling-mod progression overlays](Sibling-Overlays.md) — the optional cross-mod arc gates
- [Privacy and erasure](Privacy-and-Erasure.md)
