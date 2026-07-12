package za.co.neroland.neroagriculture.fluid;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;
import za.co.neroland.neroagriculture.platform.FluidFactory;
import za.co.neroland.nerolandcore.registry.RegistrationProvider;
import za.co.neroland.nerolandcore.registry.RegistrationProvider.RegistryEntry;

public final class ModFluids {
    public static final RegistrationProvider<Fluid> FLUIDS = RegistrationProvider.get(Registries.FLUID, NeroAgricultureCommon.MOD_ID);
    public static final RegistryEntry<Fluid> NUTRIENT = FLUIDS.register("nutrient", key -> FluidFactory.INSTANCE.createSource());
    public static final RegistryEntry<Fluid> FLOWING_NUTRIENT = FLUIDS.register("flowing_nutrient", key -> FluidFactory.INSTANCE.createFlowing());
    public static final RegistryEntry<Fluid> BIOFUEL = FLUIDS.register("biofuel", key -> FluidFactory.INSTANCE.createSource());
    public static final RegistryEntry<Fluid> FLOWING_BIOFUEL = FLUIDS.register("flowing_biofuel", key -> FluidFactory.INSTANCE.createFlowing());
    private ModFluids() { }
    public static void init() { }
}
