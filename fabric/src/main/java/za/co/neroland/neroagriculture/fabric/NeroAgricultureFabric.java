package za.co.neroland.neroagriculture.fabric;

import net.fabricmc.api.ModInitializer;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;

/** Fabric entry point for NeroAgriculture. */
public final class NeroAgricultureFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        NeroAgricultureCommon.LOGGER.info("[NeroAgriculture] Fabric bootstrap");
        NeroAgricultureCommon.init();
    }
}
