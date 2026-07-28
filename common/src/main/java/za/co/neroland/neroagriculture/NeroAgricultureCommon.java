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
        // Resolve the ServiceLoader singletons now (mirroring how ModFluids.init() forces FluidFactory)
        // so a missing platform impl fails fast at construction instead of mid-tick on first use —
        // with telemetry opted out, nothing else touches Services during init.
        java.util.Objects.requireNonNull(za.co.neroland.neroagriculture.platform.Services.NETWORK,
                "network platform");
        java.util.Objects.requireNonNull(za.co.neroland.neroagriculture.platform.Services.PLATFORM,
                "platform info");
        AgricultureRegistries.init();
        AgricultureConfig.init();
        AgricultureNetwork.init();
        za.co.neroland.neroagriculture.food.FoodProviders.register();
        za.co.neroland.neroagriculture.automation.AutomationOwner.register();
        za.co.neroland.neroagriculture.terraforming.TerraformingRegions.register();
        LOGGER.info("[NeroAgriculture] common init");
    }
}
