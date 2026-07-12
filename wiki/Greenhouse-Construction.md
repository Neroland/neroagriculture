# Greenhouse Construction

A greenhouse is a sealed room that maintains a controlled growing atmosphere — temperate, oxygenated and
pressurised — regardless of the world outside. It is what lets high-tier crops, and any crop in a hostile
world (Nether, End, or a Nerospace planet), grow at all. See [Growth Conditions](Growth-Conditions.md)
for when sealing is required.

## Building one

1. Enclose an airtight room out of any solid blocks. **Greenhouse Frame** and **Greenhouse Glass** are the
   intended shell, but any full solid block counts as a wall — there must be no gaps to the outside.
2. Place a **Greenhouse Controller** as part of the shell, touching the interior.
3. Supply the controller with Nero Flux (NF) and, if crops are inside, Nutrient fluid.

The controller flood-fills the interior air pocket, bounded by the surrounding solid blocks and capped by
the configured volume limit. Crops (and empty air) count as interior; grow beds and walls are boundary.
The check runs only when the structure changes or on a slow safety interval — never every tick.

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
