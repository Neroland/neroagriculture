package za.co.neroland.neroagriculture.platform;

import java.util.ServiceLoader;

import net.minecraft.world.level.material.Fluid;

import za.co.neroland.neroagriculture.fluid.FluidKind;

/**
 * Platform seam for building the loader-specific {@link Fluid} implementations.
 *
 * <p>Both methods take the {@link FluidKind} being built so each fluid gets its own properties —
 * its own fluid type, bucket and liquid block — rather than every fluid sharing one set.</p>
 */
public interface FluidFactory {
    FluidFactory INSTANCE = ServiceLoader.load(FluidFactory.class).findFirst()
            .orElseThrow(() -> new IllegalStateException("Missing NeroAgriculture fluid factory"));

    Fluid createSource(FluidKind kind);

    Fluid createFlowing(FluidKind kind);
}
