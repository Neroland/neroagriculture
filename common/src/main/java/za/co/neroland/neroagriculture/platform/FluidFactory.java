package za.co.neroland.neroagriculture.platform;

import java.util.ServiceLoader;

import net.minecraft.world.level.material.Fluid;

public interface FluidFactory {
    FluidFactory INSTANCE = ServiceLoader.load(FluidFactory.class).findFirst()
            .orElseThrow(() -> new IllegalStateException("Missing NeroAgriculture fluid factory"));
    Fluid createSource();
    Fluid createFlowing();
}
