package za.co.neroland.neroagriculture.crop;

import net.minecraft.world.level.material.Fluid;

import za.co.neroland.nerolandcore.energy.NeroEnergyStorage;
import za.co.neroland.nerolandcore.fluid.NeroFluidStorage;

/** Atomic simulation-before-mutation transaction for one powered-bed growth step. */
public final class GrowBedResources {
    private GrowBedResources() { }

    public static boolean has(NeroEnergyStorage energy, NeroFluidStorage fluid, Fluid nutrient,
            int energyCost, int fluidCost) {
        return (energyCost == 0 || energy.extract(energyCost, true) >= energyCost)
                && (fluidCost == 0 || fluid.getFluid() == nutrient && fluid.drain(fluidCost, true) >= fluidCost);
    }

    public static boolean consume(NeroEnergyStorage energy, NeroFluidStorage fluid, Fluid nutrient,
            int energyCost, int fluidCost) {
        if (!has(energy, fluid, nutrient, energyCost, fluidCost)) return false;
        if (energyCost > 0) energy.extract(energyCost, false);
        if (fluidCost > 0) fluid.drain(fluidCost, false);
        return true;
    }
}
