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
    }
}
