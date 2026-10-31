package za.co.neroland.neroagriculture.platform;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.world.level.material.Fluid;
//? if >=1.21.6 {
import net.minecraftforge.eventbus.api.bus.BusGroup;
//?} else {
/*import net.minecraftforge.eventbus.api.IEventBus;
*///?}
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
            //? if >=26.1 {
            TYPE_BY_KIND.put(kind, TYPES.register(kind.id(), () -> new FluidType(FluidType.Properties.create()
                    .density(kind.density()).viscosity(kind.viscosity()).canConvertToSource(false))));
            //?} else {
            /*TYPE_BY_KIND.put(kind, TYPES.register(kind.id(), () -> new TexturedFluidType(FluidType.Properties.create()
                    .density(kind.density()).viscosity(kind.viscosity()).canConvertToSource(false), kind)));
            *///?}
        }
    }

    //? if >=1.21.6 {
    public static void registerFluidTypes(BusGroup bus) {
    //?} else {
    /*public static void registerFluidTypes(IEventBus bus) {
    *///?}
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
    //? if <26.1 {
    /*// Before 26.1 Forge has no fluid-model event: a FluidType hands its still/flow sprites over from
    // initializeClient (the consumer needs its own object, not the type itself).
    private static final class TexturedFluidType extends FluidType {
        private final net.minecraft.resources.Identifier still;
        private final net.minecraft.resources.Identifier flowing;

        TexturedFluidType(Properties properties, FluidKind kind) {
            super(properties);
            this.still = net.minecraft.resources.Identifier.fromNamespaceAndPath(NeroAgricultureCommon.MOD_ID,
                    "block/" + kind.id() + "_still");
            this.flowing = net.minecraft.resources.Identifier.fromNamespaceAndPath(NeroAgricultureCommon.MOD_ID,
                    "block/" + kind.id() + "_flow");
        }

        @Override
        public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions> consumer) {
            consumer.accept(new net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions() {
                @Override public net.minecraft.resources.Identifier getStillTexture() { return TexturedFluidType.this.still; }
                @Override public net.minecraft.resources.Identifier getFlowingTexture() { return TexturedFluidType.this.flowing; }
            });
        }
    }

    *///?}
    private static ForgeFlowingFluid.Properties properties(FluidKind kind) {
        return PROPERTIES_BY_KIND.computeIfAbsent(kind, k -> new ForgeFlowingFluid.Properties(
                TYPE_BY_KIND.get(k), k.source(), k.flowing()).bucket(k.bucket()).block(k.block()));
    }
}
