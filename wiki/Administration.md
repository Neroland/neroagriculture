# Administration

Server-side tools for inspecting and managing NeroAgriculture.

## Catalog diagnostics

Operator commands (require gamemaster/permission level) print material metadata only — never player data:

- `/neroagriculture catalog list` — active vs total materials with tier and source.
- `/neroagriculture catalog show <material>` — full effective definition for one material.
- `/neroagriculture catalog errors` — datapack parse errors and conflicts.
- `/neroagriculture catalog report` — effective tier/gate/yield ramp/conversion and override source per material.

Reload logging is aggregate-first: shadowed lower-priority definitions (common on big packs, where a
datapack overrides many builtins) log one summary WARN per reload with per-material detail at DEBUG
level, and blacklist entries that match no material id log one aggregate INFO line so typos are visible.

## Showcase gallery

`/neroagriculture gallery` (creative, operator) builds the block/exhibit showcase around the player;
`/neroagriculture gallery clear` removes **exactly the blocks and label stands the gallery itself placed**
(recorded per dimension for the server session) — it never wipes pre-existing builds inside the same
footprint. See [Art and Visuals](Art-and-Visuals.md) for what the gallery demonstrates.

## Datapacks

Data-driven content is hand-authored under `data/neroagriculture/neroagriculture/`:
`materials/`, `foods/`, `environments/`, `cycles/`, and `breeding/`. See
[Material Recipe Datapacks](Material-Recipe-Datapacks.md) and the per-system pages for formats. Reloads take
effect on the next server access; catalogs are reload-safe and never reassign material ids.

## Ownership and claims

Automation and terraforming honour a claim/protection policy seam and fail closed when a position is denied.
`automation.track_owner` records only a placer UUID; see [Privacy and Erasure](Privacy-and-Erasure.md).

## Terraforming

Terraforming changes only Agriculture's environment model for a bounded region — never world blocks. A
project's owner can sneak-right-click the controller to roll back; **breaking the controller always removes
the region**, so administrators can undo any project. The feature is gated by `terraforming.enabled`.

## Player-data erasure

Milestones and machine ownership are cleared through Core's shared erasure hook; NeroAgriculture participates
automatically. See [Privacy and Erasure](Privacy-and-Erasure.md).

See also: [Configuration](Configuration.md), [Performance](Performance.md).
