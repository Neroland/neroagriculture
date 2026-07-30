package za.co.neroland.neroagriculture.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;
import za.co.neroland.neroagriculture.catalog.CatalogSync;
import za.co.neroland.neroagriculture.command.AgricultureCommands;
import za.co.neroland.neroagriculture.compat.CompatContracts;
import za.co.neroland.neroagriculture.lifecycle.ServerStateReset;
import za.co.neroland.neroagriculture.telemetry.NeroAgricultureTelemetry;

/** Fabric entry point for NeroAgriculture. */
public final class NeroAgricultureFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        NeroAgricultureCommon.LOGGER.info("[NeroAgriculture] Fabric bootstrap");
        NeroAgricultureCommon.init();
        // Anonymous, NeroAgriculture-only crash reporting (opt-out via config/neroagriculture.properties).
        NeroAgricultureTelemetry.init();
        FabricNetwork.register();
        FabricCapabilities.register();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                AgricultureCommands.register(dispatcher));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> CatalogSync.syncTo(handler.player));
        // Optional Nerospace planet-visit adapter (join backfill + tick-diff dimension tracking — this
        // fabric-api build ships no server-side player dimension-change event, so the bridge polls).
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                CompatContracts.playerJoined(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                CompatContracts.playerDisconnected(handler.player));
        ServerTickEvents.END_SERVER_TICK.register(CompatContracts::serverTick);
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            if (success) CatalogSync.reloadAndSync(server);
        });
        // Clear the common server-scoped static caches so nothing leaks into the next (single-player) world.
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ServerStateReset.serverStopped());
    }
}
