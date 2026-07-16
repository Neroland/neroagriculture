package za.co.neroland.neroagriculture.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;

/** Fabric client entry point for NeroAgriculture. */
public final class NeroAgricultureFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        NeroAgricultureCommon.LOGGER.info("[NeroAgriculture] Fabric client bootstrap");
        FabricNetwork.registerClient();
        MenuScreens.register(za.co.neroland.neroagriculture.registry.ModMenuTypes.FOUNDATION_MACHINE.get(),
                za.co.neroland.neroagriculture.client.FoundationMachineScreen::new);
        MenuScreens.register(za.co.neroland.neroagriculture.registry.ModMenuTypes.GENETICS_STATION.get(),
                za.co.neroland.neroagriculture.client.GeneticsStationScreen::new);
        MenuScreens.register(za.co.neroland.neroagriculture.registry.ModMenuTypes.CROP_TOWER_CONTROLLER.get(),
                za.co.neroland.neroagriculture.client.CropTowerScreen::new);
        MenuScreens.register(za.co.neroland.neroagriculture.registry.ModMenuTypes.STATUS_CONTROLLER.get(),
                za.co.neroland.neroagriculture.client.StatusScreen::new);
        MenuScreens.register(za.co.neroland.neroagriculture.registry.ModMenuTypes.AREA_MACHINE.get(),
                za.co.neroland.neroagriculture.client.AreaMachineScreen::new);
        MenuScreens.register(za.co.neroland.neroagriculture.registry.ModMenuTypes.CONVERTER.get(),
                za.co.neroland.neroagriculture.client.ProcessorScreen::new);
        MenuScreens.register(za.co.neroland.neroagriculture.registry.ModMenuTypes.PROCESSOR.get(),
                za.co.neroland.neroagriculture.client.ProcessorScreen::new);
        MenuScreens.register(za.co.neroland.neroagriculture.registry.ModMenuTypes.GROW_BED.get(),
                za.co.neroland.neroagriculture.client.GrowBedScreen::new);
        // Register the crop tint against vanilla BlockColors directly (no Fabric-API rendering module
        // needed): the same CropTintSource the Forge/NeoForge events use.
        net.minecraft.client.Minecraft.getInstance().getBlockColors().register(
                java.util.List.of(new za.co.neroland.neroagriculture.client.CropTintSource()),
                za.co.neroland.neroagriculture.registry.ModBlocks.RESOURCE_CROP.get());
    }
}
