package za.co.neroland.neroagriculture.platform;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;
import za.co.neroland.neroagriculture.fluid.FluidKind;

/**
 * NeoForge fluid implementations: one {@link FluidType} and one set of
 * {@link BaseFlowingFluid.Properties} per {@link FluidKind}.
 */
public final class NeoForgeFluidFactory implements FluidFactory {
    private static final DeferredRegister<FluidType> TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, NeroAgricultureCommon.MOD_ID);
    private static final Map<FluidKind, DeferredHolder<FluidType, FluidType>> TYPE_BY_KIND =
            new EnumMap<>(FluidKind.class);
    private static final Map<FluidKind, BaseFlowingFluid.Properties> PROPERTIES_BY_KIND =
            new EnumMap<>(FluidKind.class);

    static {
        for (FluidKind kind : FluidKind.values()) {
            TYPE_BY_KIND.put(kind, TYPES.register(kind.id(), () -> new FluidType(FluidType.Properties.create()
                    .density(kind.density()).viscosity(kind.viscosity()).canConvertToSource(false))));
        }
    }

    public static void registerFluidTypes(IEventBus bus) {
        TYPES.register(bus);
    }

    @Override
    public Fluid createSource(FluidKind kind) {
        return new BaseFlowingFluid.Source(properties(kind));
    }

    @Override
    public Fluid createFlowing(FluidKind kind) {
        return new BaseFlowingFluid.Flowing(properties(kind));
    }

    /**
     * Built on first use rather than in the static initialiser: the registry entries these properties
     * point at are assigned while {@code ModFluids} initialises, which happens before the fluids
     * themselves are created.
     */
    private static BaseFlowingFluid.Properties properties(FluidKind kind) {
        return PROPERTIES_BY_KIND.computeIfAbsent(kind, k -> new BaseFlowingFluid.Properties(
                TYPE_BY_KIND.get(k), k.source(), k.flowing()).bucket(k.bucket()).block(k.block()));
    }
}
