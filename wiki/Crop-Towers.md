# Automated Crop Towers

A crop tower is a vertical multiblock that farms many crops in a compact footprint, hands-free. It is
**colony-scale automation, not a shortcut** — every slot obeys the same rules as an ordinary crop, so a
tower can never out-produce or duplicate compared with an equivalent farm.

## Building a tower

Place a **Crop Tower Controller**, then stack **Crop Tower Frame** blocks directly above it. The tower's
**tier is its height**: it forms once the casing run reaches the minimum height and its capacity is
`height x slots-per-layer`, up to a configured maximum (a taller stack simply caps out). The controller
caches the formed structure and only rechecks it on a slow safety interval (a few seconds by default) — it
never scans the whole structure every tick, and a freshly stacked tower forms by itself within moments.

The controller's screen carries this build guide **in-GUI**: a panel down its left-hand column walks
through the stacking steps (with a frame/controller sketch), so the tower can be built without leaving
the game.

## Running a tower

Feed the controller through Core [side configuration](Side-Configuration.md):

- **Resource seeds** into the seed slots — the tower plants empty slots from them.
- **Nero Flux and Nutrient** — each growth step costs the same NF and nutrient as a powered grow bed.
- An optional **Speed or Yield Fertiliser** in the fertiliser slot boosts growth or harvest within the same
  caps as field fertiliser. A Yield Fertiliser dose is only consumed when a harvest actually banks its
  yield — a jammed output cluster costs nothing.
- **Resource Fragments** are deposited into the output slots on harvest — the full yield, split across as
  many stacks as needed; pull them with hoppers or pipes. A harvest is all-or-nothing: if the whole yield
  does not fit, the slot stays mature and retries once space is freed, and nothing is ever discarded. A
  zero-yield harvest still resets the slot so the tower keeps cycling.

Each cycle the controller works a bounded number of slots from a rolling cursor: it plants an empty slot,
advances a growing one (paying NF/nutrient), or harvests a mature one. Planting, the progression gate,
capped yield, and genetics/fertiliser bonuses all use the **same shared logic** as ordinary crops.

### Environment surcharge

Towers keep working in hostile environments, but not for free. Each layer runs the **same climate check
a grow bed uses** (habitability, and the sealed-greenhouse requirement for high tiers, relaxed by the
crop's hardiness genetics). Where a grow bed would refuse to grow the crop, the tower layer instead pays
an NF surcharge — the growth step costs
`grow_beds.energy_per_growth x crop_tower.hostile_environment_nf_multiplier` (default 4x). Sealing the
tower inside a powered [greenhouse](Greenhouse-Construction.md), terraforming the region, or breeding
hardier crops removes the surcharge.

## The controller screen

Alongside the slots, gauges and the `height / active slots` readout the controller shows:

- A **growth bar** summarising the planted slots: the fill is the **average growth** across every planted
  slot and the label counts how many are ready to harvest (e.g. "Ripe 3/12"). An empty tower shows the
  bare bar with a "Growth —" label, the same idiom as a bare grow bed.
- A **status line** with the tower's current blocker — progression gate closed, out of NF, out of
  nutrient, or a disabled/unknown material. Nothing planted reads as idle.
- A scrollable **seed panel** listing every material in the server's catalog. Unlike a grow bed the tower
  has **no tier gate**: any resource seed it accepts is a seed it will grow, whatever its tier. What still
  bites is checked at growth time — the progression gate, and the NF and nutrient the slot draws each
  step. The panel says so above the list rather than implying a bed-tier restriction that does not exist.

## Genetics, contents, and safety

Slots carry full [genetics](Genetics.md) and harvest history, which are preserved on save, chunk unload,
and restart. Breaking the controller returns every stored item plus a seed for each planted slot (with its
genetics and history), so nothing is lost when you take a tower down. Right-click the controller for its
status (formed/height/slots/planted/NF/nutrient).

## Performance

Work is bounded per pass and phase-offset, contents are cached, and crops are virtual slots rather than
thousands of blocks — so a tower stays cheap even at colony scale. Any crop-layer visuals are presentation
only; the server logic never depends on the renderer.

See also: [Resource Crops](Resource-Crops.md), [Automation](Automation.md), [Genetics](Genetics.md).
