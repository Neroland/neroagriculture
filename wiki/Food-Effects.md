# Food Effects

Most engineered and alien crops grant a **signature effect** when eaten. There are six documented
categories, chosen so that food cultivation directly supports survival and exploration.

| Category | In-game effect | Source |
| --- | --- | --- |
| Night vision | See in the dark | Vanilla |
| Mining haste | Faster block breaking | Vanilla |
| Fire resistance | Immunity to burning | Vanilla |
| Low-gravity adaptation | Negates fall damage while active | NeroAgriculture |
| Oxygen efficiency | Keeps your air supply topped up | NeroAgriculture |
| Freeze immunity | Prevents powder-snow/cold freezing | NeroAgriculture |

Three categories map cleanly onto existing vanilla effects. The remaining three have no clean vanilla
analogue, so NeroAgriculture registers its own effects for them. All three are fully standalone — they
never require Nerospace to be installed, though Nerospace suit/environment systems may build on them
later.

## Bounds and authority

Every effect is **server-authoritative and bounded**. Each species definition records a base amplifier
and duration plus a **potency cap** and **duration cap**; the value actually applied is the base clamped
to those caps. Because the effect is looked up from the server food catalogue at the moment of eating
(not read from the item), tampering with an item's components cannot raise potency or duration beyond
the definition. Genetics (a later stage) may strengthen or extend an effect only within the same caps.

Effects are renewed and removed by vanilla's status-effect handling, so eating again refreshes the timer
and the effect ends cleanly when it expires.

See also: [Engineered Foods](Engineered-Foods.md), [Planet Varieties](Planet-Varieties.md).
