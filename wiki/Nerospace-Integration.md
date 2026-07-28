# Nerospace Integration (Planet Visits)

Some catalog materials are **planet-bound**: their definition carries a dimension restriction in the
`nerospace` namespace (Nerospace planet ores and planet-restricted meteor materials). For these, the
[Seed Research Bench](Seed-Research.md) refuses the usual owner-mod/pickup evidence — the Core
`material_discovered` milestone can only come from **actually visiting the planet**. This page covers the
runtime-guarded adapter that records those visits.

## What it does

- **Live visits** — when a player enters a `nerospace:*` dimension (rocket landing, dimension change, or
  logging in there), the server grants `material_discovered` for every catalog material bound to that
  dimension. From then on the bench researches those materials normally.
- **Historical backfill** — on each join, if Nerospace `1.0.0-beta.8` or newer is installed, the adapter
  queries Nerospace's public visit-history API (`za.co.neroland.nerospace.api.NerospaceVisits`) once and
  grants milestones for planets visited *before* NeroAgriculture was added to the world.

Grants are idempotent — a milestone already observed is never re-written — and event-driven: there is no
polling and at most one cross-mod query per player per login.

## API floor and behavior without Nerospace

| Situation | Behavior |
| --- | --- |
| Nerospace absent | Fully dormant. Planet dimensions cannot exist, so nothing fires; planet-bound materials stay locked (they are unreachable content anyway). |
| Nerospace below `1.0.0-beta.8` | Live visit tracking works (it uses only dimension identity, no Nerospace code). Historical backfill is skipped — visit the planet once after installing NeroAgriculture. |
| Nerospace `1.0.0-beta.8`+ | Live tracking **and** historical backfill on join. |

NeroAgriculture never hard-depends on Nerospace: no manifest requires it, and the history query binds
reflectively at runtime against Nerospace's semver-stable `nerospace.api` facade only. Any absence,
version mismatch, or API error degrades silently to live tracking.

## Configuration

| Key | Default | Effect |
| --- | --- | --- |
| `compat.nerospace_visits` | `true` | Enable the adapter. When `false`, planet visits are never recorded and planet-bound materials cannot earn `material_discovered`. |

## Privacy (POPIA/GDPR)

- **What is read:** the player's current dimension (server-side, at join/dimension change) and — with
  Nerospace `1.0.0-beta.8`+ — the list of planets Nerospace already stores as visited for that player's
  UUID. Nothing else crosses the mod boundary.
- **What is stored:** only the resulting `material_discovered` milestone grants, in Neroland Core's
  `MaterialMilestones` store. NeroAgriculture keeps no shadow visit log, no timestamps, and no caches
  that outlive the query.
- **Erasure:** milestone grants live entirely in Core, so a single `PlayerDataErasure` request purges
  them along with all other research milestones; Nerospace's own visit history is erased by Nerospace's
  registered eraser. Logs carry material and dimension ids only — never player names or UUIDs.

## See also

- [Seed research](Seed-Research.md) — where the `material_discovered` milestone is consumed
- [Sibling-mod progression overlays](Sibling-Overlays.md) — the optional cross-mod arc gates
- [Privacy and erasure](Privacy-and-Erasure.md)
