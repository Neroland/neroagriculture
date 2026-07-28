package za.co.neroland.neroagriculture.neoforge;

import java.util.List;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import za.co.neroland.neroagriculture.client.CropTintSource;
import za.co.neroland.neroagriculture.client.ScreenBindings;
import za.co.neroland.neroagriculture.fluid.FluidKind;
import za.co.neroland.neroagriculture.registry.ModBlocks;

/** NeoForge client-only fabrication screen + colour-handler wiring. */
public final class NeoForgeClientSetup {
    private NeoForgeClientSetup() { }
    public static void init(IEventBus bus) {
        bus.addListener(NeoForgeClientSetup::screens);
        bus.addListener(NeoForgeClientSetup::blockTints);
        bus.addListener(NeoForgeClientSetup::fluidExtensions);
        // Clear all client-session caches on disconnect so server A's synced catalogs never leak into
        // a session on server B (or on a server without this mod, which sends no fresh snapshot).
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) ->
                        za.co.neroland.neroagriculture.lifecycle.ClientStateReset.disconnected());
    }

    /** Give every NeroAgriculture fluid its still/flow sprites (26.x fluid models, per-fluid registration). */
    private static void fluidExtensions(net.neoforged.neoforge.client.event.RegisterFluidModelsEvent event) {
        for (FluidKind kind : FluidKind.values()) {
            event.register(fluidModel(kind), kind.source(), kind.flowing());
        }
    }

    private static net.minecraft.client.renderer.block.FluidModel.Unbaked fluidModel(FluidKind kind) {
        return new net.minecraft.client.renderer.block.FluidModel.Unbaked(
                new net.minecraft.client.resources.model.sprite.Material(
                        net.minecraft.resources.Identifier.fromNamespaceAndPath("neroagriculture", "block/" + kind.id() + "_still")),
                new net.minecraft.client.resources.model.sprite.Material(
                        net.minecraft.resources.Identifier.fromNamespaceAndPath("neroagriculture", "block/" + kind.id() + "_flow")),
                null,
                (net.neoforged.neoforge.client.fluid.FluidTintSource) null);
    }
    private static void blockTints(RegisterColorHandlersEvent.BlockTintSources event) {
        event.register(List.of(new CropTintSource()), ModBlocks.RESOURCE_CROP.get());
    }
    private static void screens(RegisterMenuScreensEvent event) {
        // One canonical menu->screen table in common; adapted onto the NeoForge event here.
        ScreenBindings.registerAll(new ScreenBindings.Registrar() {
            @Override
            public <M extends AbstractContainerMenu, U extends AbstractContainerScreen<M>> void register(
                    MenuType<M> type, ScreenBindings.ScreenFactory<M, U> factory) {
                event.register(type, factory::create);
            }
        });
    }
}
