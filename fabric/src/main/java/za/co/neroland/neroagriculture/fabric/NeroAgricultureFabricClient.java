package za.co.neroland.neroagriculture.fabric;

import net.fabricmc.api.ClientModInitializer;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;

/** Fabric client entry point for NeroAgriculture. */
public final class NeroAgricultureFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        NeroAgricultureCommon.LOGGER.info("[NeroAgriculture] Fabric client bootstrap");
    }
}
