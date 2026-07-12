# Automation — Planter and Harvester

The **Planter** and **Harvester** are NF-powered machines that work a square area of grow beds around
themselves, so a flat farm can run hands-free. Both use the same bounded, interval-batched engine and never
scan the whole area every tick or force-load chunks.

## How they work

Build a flat field of grow beds around the machine at the machine's own Y level. Each work interval the
machine processes a bounded number of columns from a rolling cursor:

- The **Planter** looks for an empty bed with a spare space above it and plants a matching seed from its
  inventory, validating the same catalog, tier, gate and dimension rules as manual planting.
- The **Harvester** takes any mature crop in the area in place — the plant and its harvest history are
  preserved (no block replacement), and produce goes to its inventory or drops at the machine if full.

Work is phase-offset per position so neighbouring machines do not all fire on the same tick, a fixed number
of columns are handled per pass, unloaded columns are skipped, and each operation costs NF.

## Loading items and upgrades

Hoppers and pipes load seeds into a Planter and pull produce from a Harvester through the machine's item
faces and Core [side configuration](Side-Configuration.md). You can also right-click the machine with a
seed, a fertiliser, or an upgrade module in hand to drop it straight into a free slot; right-click with an
empty hand to read the machine's status (mode, area size, NF, owner, speed).

## Upgrades and area size

Core upgrade modules slot into every machine. **Range** modules step the work area 3×3 → 5×5 → 7×7 → 9×9
(clamped at 9×9). **Speed** modules make passes more effective and **Efficiency** modules lower the energy
cost, exactly as on the fabrication machines.

## Ownership and claims

If owner tracking is enabled (config `automation.track_owner`, on by default), a machine records only the
placing player's UUID — never a name — so a claims/protection mod can authorise its edits. Standalone, with
no claims mod, machines work freely on loaded chunks. A player-data erasure request clears the stored owner
from any loaded machine.

See also: [Fertiliser](Fertiliser.md), [Side Configuration](Side-Configuration.md).
