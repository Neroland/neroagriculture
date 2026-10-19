# NeroLink Module (Companion Clients)

NeroAgriculture registers a **link module** with Neroland Core, so a companion client can show you your
own farm while you are away from the game. The module is read-mostly, scoped strictly to machines you
own, and entirely optional.

NeroAgriculture ships no server, no network protocol and no app of its own. It publishes what it can
show and what it can do into Core's registry; the separate **NeroLink** bridge mod is what serves that
to a client. With no bridge installed, this whole feature costs one registry entry and nothing else.

- **Module id:** `neroagriculture`
- **Schema version:** `1`
- **Config:** `link.module_enabled` (default `true`)

## What a client can see

| Section | Contents |
| --- | --- |
| `crops` | One row per grow bed **you own**: tier, stored NeroFlux and nutrient, whether it can afford to grow, whether it is inside a sealed greenhouse, what is planted above it (material, age, growth %, ready), and the current growth blocker if there is one. |
| `machines` | Your Fragment Extractor / Infuser / Seed Synthesizer / Seed Research Bench (kind, state, progress), your Planter / Harvester / Fertiliser Applicator (mode, power), and your Terraforming Controllers (stage, progress, radius). |
| `greenhouse` | The greenhouses that **enclose your grow beds**: seal state, volume, crop count, oxygen, power and nutrient, and where the leak is when one is breached. |
| `fragments` | Your progress up the Cosmic-ascent ladder — Territe → Forgite → Orbite → Colonite → Voidite. Per tier: how many catalog materials you have discovered out of how many exist, and whether that tier's gate is open. |

## What a client can do

| Action | Effect | Works offline |
| --- | --- | --- |
| `acknowledge_alert` | Marks one of your own NeroAgriculture alerts read in Core's alert store. | Yes |

**That is the entire write surface, and deliberately so.** Everything else a phone might want to do to a
farm — plant, harvest, fertilise, start or cancel a terraforming project, move items between slots —
either creates items, consumes items, or changes the world. A farm is an item factory: every write worth
having is a write that produces or spends something, which is exactly the kind of action that should
require being in the world. No action in this module can plant, harvest, or alter anybody's crops,
including your own.

## Live events

| Topic | When | Alert |
| --- | --- | --- |
| `greenhouse` | A greenhouse enclosing one of your grow beds changes seal state (formed / breached / unpowered / unformed). | Yes — a warning when it stops being sealed, at most once per greenhouse per five minutes. |
| `terraforming` | One of your terraforming projects reaches a new stage, including completing. | No. |

Every event is **owner-scoped** — routed to the player it concerns and nobody else. There are no
server-wide broadcasts: a farm is not a public place, and a greenhouse breach is nobody else's business.

Crop growth is deliberately not an event. Grow beds auto-harvest and auto-replant, so "your crop is
ready" is not something you need to act on, and one notification per crop per stage would be noise.
Growth is standing state and lives in the `crops` section instead.

## Why there is no `towers` section

The **Crop Tower Controller records no owner**. In-world it decides who to check gates against with
"nearest player within 24 blocks", which is a fair approximation of *who is standing here operating
this* and a completely unacceptable one for *who may read this from the internet* — a player idling near
somebody else's tower would be handed its contents. There is no enclosing structure to inherit ownership
from either: a tower is a controller and a frame column, and belongs to nothing.

So towers are left out rather than guessed at. When the controller records a placing player the way grow
beds and the other machines do, `towers` becomes an additive section and a schema bump.

The `greenhouse` section passes the same test only because it is scoped **indirectly**. A greenhouse
controller has no owner either, so a greenhouse is reported to you only when it encloses a grow bed you
own. The permission comes from the bed — which really does carry your UUID — and the greenhouse is
simply the building your bed is standing in.

## Privacy (POPIA/GDPR)

- **Own machines only.** Every section is scoped to the requesting player's UUID before anything leaves
  the mod. Nothing is reported about any other player: not their machines, not their names, not that
  they exist. The module never widens for operators — an operator's powers belong to a live command
  source, not to a UUID arriving over a bridge.
- **Loaded machines only.** A machine in an unloaded chunk is simply absent from the snapshot. Answering
  a phone is not a reason to load somebody's base, and no section can force-load a chunk.
- **Coordinates.** A machine's own position is reported, because it is the only way an app can tell two
  of your farms apart, and it is your own block. The greenhouse section adds the enclosing controller's
  position and its leak — a building you have a machine standing inside. Nothing else carries a position.
- **Aggregates stay aggregate.** A shared greenhouse reports how many crops are inside, never whose.
- **Erasure needs no separate wiring.** Every read starts from the live owner UUID on a block entity, so
  a player erased through Core's `PlayerDataErasure` hook immediately owns nothing and every section
  answers empty for them. See [Privacy and erasure](Privacy-and-Erasure.md).
- **Off means off.** With `link.module_enabled = false` nothing is registered at all — no snapshot, no
  action and no event ever leaves the mod, and companion clients simply do not see a NeroAgriculture
  section.

## For administrators

| Key | Default | Effect |
| --- | --- | --- |
| `link.module_enabled` | `true` | Register the module with Core. `false` registers nothing; every publisher checks the same flag before it speaks. |

A failure while registering the module is logged and swallowed — farming is unaffected and the server
starts normally. If companion clients cannot see NeroAgriculture, check the server log at startup for a
`Could not register the NeroLink module` warning, and check that the NeroLink bridge mod is installed:
without it, nothing reads Core's registry.

## See also

- [Privacy and erasure](Privacy-and-Erasure.md)
- [Configuration](Configuration.md)
- [Administration](Administration.md)
- [Terraforming](Terraforming.md)
- [Greenhouse construction](Greenhouse-Construction.md)
