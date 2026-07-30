package za.co.neroland.neroagriculture.platform;

import net.minecraft.world.level.material.Fluid;

import za.co.neroland.neroagriculture.fluid.FluidKind;
import za.co.neroland.neroagriculture.fluid.ModFlowingFluid;

/** Fabric fluid implementations: a plain {@link ModFlowingFluid} carrying its own {@link FluidKind}. */
public final class FabricFluidFactory implements FluidFactory {
    @Override public Fluid createSource(FluidKind kind) { return new ModFlowingFluid.Source(kind); }
    @Override public Fluid createFlowing(FluidKind kind) { return new ModFlowingFluid.Flowing(kind); }
}
