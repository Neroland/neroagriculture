# Greenhouse Construction

A greenhouse is a sealed room that maintains a controlled growing atmosphere — temperate, oxygenated and
pressurised — regardless of the world outside. It is what lets high-tier crops, and any crop in a hostile
world (Nether, End, or a Nerospace planet), grow at all. See [Growth Conditions](Growth-Conditions.md)
for when sealing is required.

## Building one

1. Enclose an airtight room out of any solid blocks. **Greenhouse Frame** and **Greenhouse Glass** are the
   intended shell, but any full solid block counts as a wall — there must be no gaps to the outside.
2. Fit a **Greenhouse Door** in a wall so you can walk in and out — as an airlock it never breaks the
   seal (see below).
3. Place a **Greenhouse Controller** as part of the shell, touching the interior.
4. Supply the controller with Nero Flux (NF) and, if crops are inside, Nutrient fluid.

The controller flood-fills the interior air pocket, bounded by the surrounding solid blocks and capped by
the configured volume limit. Crops (and empty air) count as interior; grow beds and walls are boundary.
The check runs only when the structure changes or on a slow safety interval — never every tick.

## The Greenhouse Door (airlock)

The **Greenhouse Door** is a two-block-tall metal-and-glass door, opened and closed by hand like a wooden
door (no redstone needed). It is the intended way in and out of a sealed greenhouse.

**Airlock seal semantics:** both halves of the door count as valid sealing shell blocks **whether the door
is open or closed**. The enclosure check treats the door itself as a wall in every state, so opening it to
walk through never breaches the greenhouse, and revalidation will never flag an open door as a leak. (An
open door still occupies its block — the interior fill stops at it either way.)

**Crafting** — shaped, yields 3 doors (a glazed upper panel on a framed base):

| | |
| --- | --- |
| Greenhouse Glass | Greenhouse Glass |
| Greenhouse Glass | Greenhouse Glass |
| Greenhouse Frame | Greenhouse Frame |

Like any door, it drops itself when the lower half is broken, and it is part of the vanilla `doors` tags.

## Powering and upkeep

While formed, the controller spends NF scaled to the interior volume and Nutrient fluid scaled to the
number of crops inside, each upkeep interval. If it cannot pay, the greenhouse drops to **Unpowered** and
its interior stops being controlled until power returns. All rates and the volume cap are server config
values under the `greenhouse.*` keys.

## Status

Right-click the controller to read its state (`formed`, `breached`, `unpowered`, or `unformed`), interior
volume, active crop count, stored NF and Nutrient, and — if breached — the leak position that pushed the
fill past the cap.

See also: [Greenhouse Troubleshooting](Greenhouse-Troubleshooting.md).
