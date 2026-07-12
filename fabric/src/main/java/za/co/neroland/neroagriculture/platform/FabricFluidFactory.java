package za.co.neroland.neroagriculture.platform;

import net.minecraft.world.level.material.Fluid;
import za.co.neroland.neroagriculture.fluid.NutrientFluid;

public final class FabricFluidFactory implements FluidFactory {
    @Override public Fluid createSource() { return new NutrientFluid.Source(); }
    @Override public Fluid createFlowing() { return new NutrientFluid.Flowing(); }
}
