# Terraforming

Terraforming makes a hostile region habitable for open-air farming. In NeroAgriculture it changes only the
mod's own **environment model** for a bounded area — it never rewrites world blocks or a planetary
atmosphere — so it is safe, reversible, and can never corrupt a save.

## For players

1. Place a **Terraforming Controller** at the centre of the area you want to convert.
2. Supply it with Nero Flux and Nutrient.
3. **Use a Terraforming Seed** on the controller to start the project.

The project advances through staged transitions (Seeded → Growing → Stabilising → Complete) at a
rate-limited pace, spending NF and Nutrient as it goes. Right-click the controller with an empty hand to
read its stage and progress. Once **Complete**, the bounded region around the controller becomes habitable
in Agriculture's environment model: crops that previously needed a sealed [greenhouse](Greenhouse-Construction.md)
can now grow open-air there. Optional [drone assistance](Automation.md), if a provider is installed, can
speed the work within bounded limits.

## Rolling back

**Sneak-right-click** the controller to roll the project back if you started it — the region override is
removed and the area returns to its natural (hostile) baseline. Breaking the controller also rolls the
project back. Because terraforming only toggles an environment override, rollback is instant and lossless.

## For administrators

- The whole feature is gated by `terraforming.enabled`; region radius and pacing are config values under
  `terraforming.*`.
- Projects respect the claim/ownership policy: with a claims mod present, only authorised players can start
  or advance a project, and the check **fails closed** when authorisation cannot be determined.
- Progress, completion and rollback are published as public terraforming events for Quests/Colonies/Events.

## Griefing warning

Terraforming an area changes growth conditions for everyone who farms there. On shared servers, restrict it
with a claims/protection mod (the policy seam is honoured) and keep `terraforming.enabled` off in regions
you do not want altered. Only the project owner can start or sneak-roll-back a project; breaking the
controller (which admins can always do) removes the region entirely.

See also: [Growth Conditions](Growth-Conditions.md), [Greenhouse Construction](Greenhouse-Construction.md).
