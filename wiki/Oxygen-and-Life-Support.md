# Oxygen and Life Support

A powered [greenhouse](Greenhouse-Construction.md) keeps its interior breathable by spending Nero Flux and
Nutrient. **Oxygen flora** and the **bioreactor** let a farm supply that life support biologically instead,
closing the loop.

## Oxygen flora

Oxygen flora are ordinary food-species crops that carry an `oxygen_production` value. Grown inside a
greenhouse, each one contributes oxygen scaled by its **maturity** (a seedling contributes nothing; a mature
plant contributes its full base) plus its **oxygen-output [genetics](Genetics.md)** trait. Two built-in
flora ship — Earth Algae and Greenxertz Oxyvine — and datapacks can flag any food species as flora with an
`oxygen_production` field.

The greenhouse sums the oxygen of its interior crops, clamps it to a **hard per-volume cap** (so a swarm of
plants can never yield unlimited free upkeep), and uses it to **offset upkeep**: oxygen covers Nutrient
demand first, then reduces the NF cost. A well-planted greenhouse can become largely self-sustaining. Oxygen
is a cost reducer — it never gates growth — and the controller's right-click status now shows the current
oxygen figure.

## The closed loop

The **Bioreactor** (the Oxygen Plant machine) turns farmed **Biomass** into **Nutrient** fluid, spending NF,
and periodically yields a recoverable **Crop Waste** byproduct. The conversion is deliberately lossy, so the
farm → biomass → nutrient → flora → oxygen loop can never mint net items or energy from nothing. Piping that
Nutrient back into powered beds feeds more oxygen flora, which in turn lower the greenhouse's upkeep.

## Nerospace and standalone

Greenhouse oxygen is published through a public seam (`OxygenApi`). When Nerospace is installed it can
consume those contributions to feed its atmosphere/terraforming truth; standalone there are no consumers and
the greenhouse simply uses this local life-support model. Nothing here depends on Nerospace being present.

See also: [Greenhouse Construction](Greenhouse-Construction.md), [Genetics](Genetics.md),
[Fertiliser](Fertiliser.md).
