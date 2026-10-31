package za.co.neroland.neroagriculture.fabric;

import net.fabricmc.api.ClientModInitializer;
//? if >=26.1 {
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.minecraft.client.color.block.BlockTintSource;
//?} else {
/*import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
*///?}
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
//? if >=26.1 {
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
//?}
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
        // Keep the client's copy of the server's synced fabrication recipes for recipe viewers
        // (compat.jei / compat.emi); 26.x clients hold no full recipe list of their own.
        // Fabric API's recipe sync starts at 1.21.9; 1.21.1 still receives every recipe (ViewerRecipes reads
        // the client level there), and on 1.21.2 - 1.21.8 Fabric clients get no fabrication recipes to list.
        //? if >=1.21.9 {
        net.fabricmc.fabric.api.client.recipe.v1.sync.ClientRecipeSynchronizedEvent.EVENT.register(
                (client, synchronizedRecipes) ->
                        za.co.neroland.neroagriculture.compat.viewer.ViewerRecipes.accept(synchronizedRecipes.recipes()));
        //?}
        // One canonical menu->screen table in common; adapted onto vanilla MenuScreens.register here.
        ScreenBindings.registerAll(new ScreenBindings.Registrar() {
            @Override
            public <M extends AbstractContainerMenu, U extends AbstractContainerScreen<M>> void register(
                    MenuType<M> type, ScreenBindings.ScreenFactory<M, U> factory) {
                MenuScreens.register(type, factory::create);
            }
        });
        // Register the crop tint through Fabric API's BlockColorRegistry: the same CropTintSource the
        // Forge/NeoForge events use. Minecraft.getBlockColors() is still null when client entrypoints
        // run (vanilla builds BlockColors later in the Minecraft constructor), so a direct call NPEs at
        // startup; the registry queues the source and applies it once BlockColors exists.
        //? if >=26.1 {
        BlockColorRegistry.register(
                java.util.List.of(new za.co.neroland.neroagriculture.client.CropTintSource()),
                za.co.neroland.neroagriculture.registry.ModBlocks.RESOURCE_CROP.get());
        //?} else {
        /*ColorProviderRegistry.BLOCK.register(new za.co.neroland.neroagriculture.client.CropTintSource(),
                za.co.neroland.neroagriculture.registry.ModBlocks.RESOURCE_CROP.get());
        *///?}
        //? if <1.21.4 {
        /*// 1.21.1 tints item stacks through colour providers (1.21.4+ item models read the custom model data).
        ColorProviderRegistry.ITEM.register(za.co.neroland.neroagriculture.client.MaterialItemTint::color,
                za.co.neroland.neroagriculture.registry.ModItems.RESOURCE_FRAGMENT.get(),
                za.co.neroland.neroagriculture.registry.ModItems.RESOURCE_SEED.get());
        *///?}
        registerFluidModels();
    }

    /**
     * Give every NeroAgriculture fluid its still/flow sprites. 26.x renders fluids from a
     * {@link FluidModel}; without one the fluid falls back to the missing-texture model. The
     * still/flowing pair shares one model, matching the Forge and NeoForge registrations.
     */
    private static void registerFluidModels() {
        for (FluidKind kind : FluidKind.values()) {
            //? if >=26.1 {
            FluidRenderingRegistry.register(kind.source().get(), kind.flowing().get(), new FluidModel.Unbaked(
                    new Material(Identifier.fromNamespaceAndPath(NeroAgricultureCommon.MOD_ID, "block/" + kind.id() + "_still")),
                    new Material(Identifier.fromNamespaceAndPath(NeroAgricultureCommon.MOD_ID, "block/" + kind.id() + "_flow")),
                    (Material) null,
                    (BlockTintSource) null));
            //?} else {
            /*FluidRenderHandlerRegistry.INSTANCE.register(kind.source().get(), kind.flowing().get(),
                    new SimpleFluidRenderHandler(
                            Identifier.fromNamespaceAndPath(NeroAgricultureCommon.MOD_ID, "block/" + kind.id() + "_still"),
                            Identifier.fromNamespaceAndPath(NeroAgricultureCommon.MOD_ID, "block/" + kind.id() + "_flow")));
            *///?}
        }
    }
}
