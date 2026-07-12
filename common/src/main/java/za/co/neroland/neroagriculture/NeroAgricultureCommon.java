package za.co.neroland.neroagriculture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import za.co.neroland.neroagriculture.config.AgricultureConfig;
import za.co.neroland.neroagriculture.network.AgricultureNetwork;
import za.co.neroland.neroagriculture.registry.AgricultureRegistries;

/**
 * Loader-agnostic entry point for NeroAgriculture. Each loader entry point
 * (Fabric / Forge / NeoForge) calls {@link #init()} once during mod
 * construction. Shared registrations and contracts live here; loader-specific
 * networking and capability exposure are reached through narrow platform seams.
 */
public final class NeroAgricultureCommon {

    public static final String MOD_ID = "neroagriculture";
    public static final Logger LOGGER = LoggerFactory.getLogger("NeroAgriculture");

    private NeroAgricultureCommon() {
    }

    /** Called once per loader during mod construction. */
    public static void init() {
        AgricultureRegistries.init();
        AgricultureConfig.init();
        AgricultureNetwork.init();
        za.co.neroland.neroagriculture.food.FoodProviders.register();
        LOGGER.info("[NeroAgriculture] common init");
    }
}
