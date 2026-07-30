# Greenhouse Troubleshooting

Right-click the **Greenhouse Controller** to see its status, then match it below.

## Unformed

The controller could not find a sealed interior. Causes:

- A gap in the shell — the fill escaped and no bounded pocket was found.
- The controller is surrounded by solid blocks with no interior air beside it.

Fix the hole so the room is airtight and wait for the next validation pass (a few seconds), or nudge the
controller by breaking and replacing it to revalidate immediately.

## Breached

The interior fill ran past the configured volume cap — the room is either genuinely too large or has a
leak to the outside. The reported **leak** position is where the fill crossed the cap; start looking for a
gap near there, or make the room smaller. The volume cap is the `greenhouse.volume_cap` server setting.

## Unpowered

The room is sealed but the controller cannot pay its upkeep. Supply more Nero Flux, and add Nutrient fluid
if crops are inside (nutrient use scales with the number of crops). Larger interiors cost more NF per
interval.

## Formed, but a crop still will not grow

A formed, powered greenhouse only helps crops **inside** its interior. Confirm the crop block sits within
the sealed pocket (on a grow bed whose tier meets the crop) and that the controller reads `formed`, not
`unpowered`. Remember the other [growth conditions](Growth-Conditions.md) still apply: light level, the
material gate, bed tier, and powered-bed NF/nutrient for Forgite-and-above crops.

See also: [Greenhouse Construction](Greenhouse-Construction.md).
