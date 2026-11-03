# Fertiliser

Fertiliser is a consumable resource sink that boosts a grow bed for a limited time. There are two
independent formulations, and each is capped so effects can never stack out of control.

| Fertiliser | Effect | Cap |
| --- | --- | --- |
| Speed Fertiliser | Crops on the bed grow in larger age steps | `fertiliser.max_dose` |
| Yield Fertiliser | Harvests from the bed produce extra output | `fertiliser.max_dose` |

Doses are stored compactly on the bed itself with an amount and an expiry time — there is no per-tick
full-farm scan. Speed and yield are tracked separately, so applying one never inflates the other, and
re-applying refreshes the timer without exceeding the cap. Fertiliser can also improve passive Territe beds.

## Making fertiliser

The **Fertiliser Processor** is an NF-powered machine that turns Biomass and Crop Waste into base
Fertiliser, with item I/O through hoppers/pipes and Core side configuration. Base Fertiliser is then
crafted into Speed or Yield Fertiliser. (In this beta, Biomass, Crop Waste and Fertiliser also have simple
crafting recipes so the loop is usable before Stage 12 adds crop-derived biomass sources.)

## Applying fertiliser

Use speed or yield fertiliser on a bed by hand, or use the **Fertiliser Applicator** —
an area machine that doses grow beds in its 3×3–9×9 range, consuming one fertiliser item per bed
and spending NF per application. It obeys the same ownership/claim checks, loaded-chunk limits, and exact
item accounting as the Planter and Harvester.

See also: [Automation](Automation.md), [Grow Beds](Grow-Beds.md).
