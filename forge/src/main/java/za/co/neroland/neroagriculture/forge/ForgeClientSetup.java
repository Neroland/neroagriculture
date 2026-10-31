package za.co.neroland.neroagriculture.forge;

//? if >=26.1 {
import net.minecraft.client.color.block.BlockTintSource;
//?}
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
//? if >=26.1 {
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
//?}
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
//? if >=26.1 {
import net.minecraftforge.client.event.ModelEvent;
//?}
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
//? if >=1.21.6 {
import net.minecraftforge.eventbus.api.bus.BusGroup;
//?} else {
/*import net.minecraftforge.eventbus.api.IEventBus;
*///?}
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;
import za.co.neroland.neroagriculture.client.CropTintSource;
import za.co.neroland.neroagriculture.client.ScreenBindings;
import za.co.neroland.neroagriculture.fluid.FluidKind;
import za.co.neroland.neroagriculture.registry.ModBlocks;

/** Forge client-only fabrication screen, colour-handler and fluid-model wiring. */
public final class ForgeClientSetup {
    private ForgeClientSetup() { }
    //? if >=26.1 {
    public static void init(BusGroup bus) {
        RegisterColorHandlersEvent.Block.BUS.addListener(event ->
                event.getBlockColors().register(java.util.List.of(new CropTintSource()), ModBlocks.RESOURCE_CROP.get()));
        ModelEvent.BakeFluidModels.BUS.addListener(ForgeClientSetup::fluidModels);
    //?}
    //? if >=1.21.9 && <26.1 {
    /*public static void init(BusGroup bus) {
        // Before 26.1 the fluid sprites come from the fluid types (ForgeFluidFactory) and tints are BlockColors.
        RegisterColorHandlersEvent.Block.BUS.addListener(event ->
                event.register(new CropTintSource(), ModBlocks.RESOURCE_CROP.get()));
    *///?}
    //? if >=1.21.6 && <1.21.9 {
    /*public static void init(BusGroup bus) {
        // Forge 1.21.6 - 1.21.8: mod-bus events are reached through their bus group.
        RegisterColorHandlersEvent.Block.getBus(bus).addListener(event ->
                event.register(new CropTintSource(), ModBlocks.RESOURCE_CROP.get()));
    *///?}
    //? if >=1.21.6 {
        // Clear all client-session caches on disconnect so server A's synced catalogs never leak into
        // a session on server B (or on a server without this mod, which sends no fresh snapshot).
        net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut.BUS.addListener(event ->
                za.co.neroland.neroagriculture.lifecycle.ClientStateReset.disconnected());
        FMLClientSetupEvent.getBus(bus).addListener(event -> event.enqueueWork(ForgeClientSetup::registerScreens));
    }
    //?} else {
    /*public static void init(IEventBus bus) {
        // Forge before 1.21.6 runs EventBus 6: mod events on the mod bus, game events on MinecraftForge.EVENT_BUS.
        bus.addListener((RegisterColorHandlersEvent.Block event) ->
                event.register(new CropTintSource(), ModBlocks.RESOURCE_CROP.get()));
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(
                (net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) ->
                        za.co.neroland.neroagriculture.lifecycle.ClientStateReset.disconnected());
        bus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(ForgeClientSetup::registerScreens));
    *///?}
    //? if <1.21.4 {
        /*// 1.21.1 tints item stacks through colour handlers (1.21.4+ item models read the custom model data).
        bus.addListener((RegisterColorHandlersEvent.Item event) ->
                event.register(za.co.neroland.neroagriculture.client.MaterialItemTint::color,
                        za.co.neroland.neroagriculture.registry.ModItems.RESOURCE_FRAGMENT.get(),
                        za.co.neroland.neroagriculture.registry.ModItems.RESOURCE_SEED.get()));
    *///?}
    //? if <1.21.6 {
    /*}
    *///?}

    private static void registerScreens() {
        // One canonical menu->screen table in common; adapted onto vanilla MenuScreens.register here.
        ScreenBindings.registerAll(new ScreenBindings.Registrar() {
            @Override
            public <M extends AbstractContainerMenu, U extends AbstractContainerScreen<M>> void register(
                    MenuType<M> type, ScreenBindings.ScreenFactory<M, U> factory) {
                MenuScreens.register(type, factory::create);
            }
        });
    }

    /**
     * Give every NeroAgriculture fluid its still/flow sprites. 26.x renders fluids from a
     * {@link FluidModel}; without one the fluid falls back to the missing-texture model. Forge hands us
     * the {@code MaterialBaker}, so the model is baked here rather than registered unbaked.
     */
    //? if >=26.1 {
    private static void fluidModels(ModelEvent.BakeFluidModels event) {
        for (FluidKind kind : FluidKind.values()) {
            FluidModel model = fluidModel(kind).bake(event.materials(), () -> "NeroAgriculture " + kind.id());
            event.register(kind.source().get(), model);
            event.register(kind.flowing().get(), model);
        }
    }

    private static FluidModel.Unbaked fluidModel(FluidKind kind) {
        return new FluidModel.Unbaked(
                new Material(Identifier.fromNamespaceAndPath(NeroAgricultureCommon.MOD_ID, "block/" + kind.id() + "_still")),
                new Material(Identifier.fromNamespaceAndPath(NeroAgricultureCommon.MOD_ID, "block/" + kind.id() + "_flow")),
                (Material) null,
                (BlockTintSource) null);
    }
    //?}
}
