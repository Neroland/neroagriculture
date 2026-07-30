package za.co.neroland.neroagriculture.platform;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;
import za.co.neroland.neroagriculture.fluid.FluidKind;

/**
 * Forge fluid implementations: one {@link FluidType} and one set of
 * {@link ForgeFlowingFluid.Properties} per {@link FluidKind}.
 */
public final class ForgeFluidFactory implements FluidFactory {
    private static final DeferredRegister<FluidType> TYPES =
            DeferredRegister.create(ForgeRegistries.FLUID_TYPES, NeroAgricultureCommon.MOD_ID);
    private static final Map<FluidKind, RegistryObject<FluidType>> TYPE_BY_KIND =
            new EnumMap<>(FluidKind.class);
    private static final Map<FluidKind, ForgeFlowingFluid.Properties> PROPERTIES_BY_KIND =
            new EnumMap<>(FluidKind.class);

    static {
        for (FluidKind kind : FluidKind.values()) {
            TYPE_BY_KIND.put(kind, TYPES.register(kind.id(), () -> new FluidType(FluidType.Properties.create()
                    .density(kind.density()).viscosity(kind.viscosity()).canConvertToSource(false))));
        }
    }

    public static void registerFluidTypes(BusGroup bus) {
        TYPES.register(bus);
    }

    @Override
    public Fluid createSource(FluidKind kind) {
        return new ForgeFlowingFluid.Source(properties(kind));
    }

    @Override
    public Fluid createFlowing(FluidKind kind) {
        return new ForgeFlowingFluid.Flowing(properties(kind));
    }

    /**
     * Built on first use rather than in the static initialiser: the registry entries these properties
     * point at are assigned while {@code ModFluids} initialises, which happens before the fluids
     * themselves are created.
     */
    private static ForgeFlowingFluid.Properties properties(FluidKind kind) {
        return PROPERTIES_BY_KIND.computeIfAbsent(kind, k -> new ForgeFlowingFluid.Properties(
                TYPE_BY_KIND.get(k), k.source(), k.flowing()).bucket(k.bucket()).block(k.block()));
    }
}
