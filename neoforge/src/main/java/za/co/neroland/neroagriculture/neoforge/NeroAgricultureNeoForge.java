package za.co.neroland.neroagriculture.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;

/** NeoForge entry point for NeroAgriculture. */
@Mod(NeroAgricultureCommon.MOD_ID)
public final class NeroAgricultureNeoForge {

    public NeroAgricultureNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        NeroAgricultureCommon.LOGGER.info("[NeroAgriculture] NeoForge bootstrap");
        NeroAgricultureCommon.init();
    }
}
