package za.co.neroland.neroagriculture.platform;

import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;
import za.co.neroland.neroagriculture.fluid.ModFluids;
import za.co.neroland.neroagriculture.registry.ModBlocks;
import za.co.neroland.neroagriculture.registry.ModItems;

public final class ForgeFluidFactory implements FluidFactory {
    private static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(ForgeRegistries.FLUID_TYPES, NeroAgricultureCommon.MOD_ID);
    private static final RegistryObject<FluidType> TYPE = TYPES.register("nutrient", () -> new FluidType(
            FluidType.Properties.create().density(1050).viscosity(1100).canConvertToSource(false)));
    private static final ForgeFlowingFluid.Properties PROPERTIES = new ForgeFlowingFluid.Properties(TYPE,
            ModFluids.NUTRIENT, ModFluids.FLOWING_NUTRIENT).bucket(ModItems.NUTRIENT_BUCKET).block(ModBlocks.NUTRIENT);
    public static void registerFluidTypes(BusGroup bus) { TYPES.register(bus); }
    @Override public Fluid createSource() { return new ForgeFlowingFluid.Source(PROPERTIES); }
    @Override public Fluid createFlowing() { return new ForgeFlowingFluid.Flowing(PROPERTIES); }
}
