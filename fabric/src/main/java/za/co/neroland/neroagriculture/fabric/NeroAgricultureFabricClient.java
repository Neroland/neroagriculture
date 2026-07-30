package za.co.neroland.neroagriculture.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;
import za.co.neroland.neroagriculture.client.ScreenBindings;
import za.co.neroland.neroagriculture.fluid.FluidKind;

/** Fabric client entry point for NeroAgriculture. */
public final class NeroAgricultureFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        NeroAgricultureCommon.LOGGER.info("[NeroAgriculture] Fabric client bootstrap");
        FabricNetwork.registerClient();
        // Clear all client-session caches on disconnect so server A's synced catalogs never leak into
        // a session on server B (or on a server without this mod, which sends no fresh snapshot).
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register(
                (handler, client) -> za.co.neroland.neroagriculture.lifecycle.ClientStateReset.disconnected());
        // One canonical menu->screen table in common; adapted onto vanilla MenuScreens.register here.
        ScreenBindings.registerAll(new ScreenBindings.Registrar() {
            @Override
            public <M extends AbstractContainerMenu, U extends AbstractContainerScreen<M>> void register(
                    MenuType<M> type, ScreenBindings.ScreenFactory<M, U> factory) {
                MenuScreens.register(type, factory::create);
            }
        });
        // Register the crop tint against vanilla BlockColors directly (no Fabric-API rendering module
        // needed): the same CropTintSource the Forge/NeoForge events use.
        net.minecraft.client.Minecraft.getInstance().getBlockColors().register(
                java.util.List.of(new za.co.neroland.neroagriculture.client.CropTintSource()),
                za.co.neroland.neroagriculture.registry.ModBlocks.RESOURCE_CROP.get());
        registerFluidModels();
    }

    /**
     * Give every NeroAgriculture fluid its still/flow sprites. 26.x renders fluids from a
     * {@link FluidModel}; without one the fluid falls back to the missing-texture model. The
     * still/flowing pair shares one model, matching the Forge and NeoForge registrations.
     */
    private static void registerFluidModels() {
        for (FluidKind kind : FluidKind.values()) {
            FluidRenderingRegistry.register(kind.source().get(), kind.flowing().get(), new FluidModel.Unbaked(
                    new Material(Identifier.fromNamespaceAndPath(NeroAgricultureCommon.MOD_ID, "block/" + kind.id() + "_still")),
                    new Material(Identifier.fromNamespaceAndPath(NeroAgricultureCommon.MOD_ID, "block/" + kind.id() + "_flow")),
                    (Material) null,
                    (BlockTintSource) null));
        }
    }
}
