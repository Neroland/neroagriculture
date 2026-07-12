package za.co.neroland.neroagriculture.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;
import za.co.neroland.neroagriculture.catalog.CatalogSync;
import za.co.neroland.neroagriculture.command.AgricultureCommands;
import za.co.neroland.nerolandcore.registry.RegistrationProvider;

/** NeoForge entry point for NeroAgriculture. */
@Mod(NeroAgricultureCommon.MOD_ID)
public final class NeroAgricultureNeoForge {

    public NeroAgricultureNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        NeroAgricultureCommon.LOGGER.info("[NeroAgriculture] NeoForge bootstrap");
        NeroAgricultureCommon.init();
        za.co.neroland.neroagriculture.platform.NeoForgeFluidFactory.registerFluidTypes(modEventBus);
        RegistrationProvider.attach(modEventBus);
        NeoForgeNetwork.register(modEventBus);
        NeoForgeCapabilities.register(modEventBus);
        if (FMLEnvironment.getDist() == Dist.CLIENT) NeoForgeClientSetup.init(modEventBus);
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) ->
                AgricultureCommands.register(event.getDispatcher()));
        NeoForge.EVENT_BUS.addListener((OnDatapackSyncEvent event) -> {
            if (event.getPlayer() == null) CatalogSync.reloadAndSync(event.getPlayerList().getServer());
            else CatalogSync.syncTo(event.getPlayer());
        });
    }
}
