package za.co.neroland.neroagriculture.platform;

import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;
import za.co.neroland.neroagriculture.fluid.ModFluids;
import za.co.neroland.neroagriculture.registry.ModBlocks;
import za.co.neroland.neroagriculture.registry.ModItems;

public final class NeoForgeFluidFactory implements FluidFactory {
    private static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, NeroAgricultureCommon.MOD_ID);
    private static final DeferredHolder<FluidType, FluidType> TYPE = TYPES.register("nutrient", () -> new FluidType(
            FluidType.Properties.create().density(1050).viscosity(1100).canConvertToSource(false)));
    private static final BaseFlowingFluid.Properties PROPERTIES = new BaseFlowingFluid.Properties(TYPE,
            ModFluids.NUTRIENT, ModFluids.FLOWING_NUTRIENT).bucket(ModItems.NUTRIENT_BUCKET).block(ModBlocks.NUTRIENT);
    public static void registerFluidTypes(IEventBus bus) { TYPES.register(bus); }
    @Override public Fluid createSource() { return new BaseFlowingFluid.Source(PROPERTIES); }
    @Override public Fluid createFlowing() { return new BaseFlowingFluid.Flowing(PROPERTIES); }
}
