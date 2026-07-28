package za.co.neroland.neroagriculture.forge;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;

import net.minecraft.server.level.ServerPlayer;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;
import za.co.neroland.neroagriculture.catalog.CatalogSync;
import za.co.neroland.neroagriculture.command.AgricultureCommands;
import za.co.neroland.neroagriculture.compat.CompatContracts;
import za.co.neroland.neroagriculture.lifecycle.ServerStateReset;
import za.co.neroland.neroagriculture.telemetry.NeroAgricultureTelemetry;
import za.co.neroland.nerolandcore.registry.RegistrationProvider;

/** MinecraftForge entry point for NeroAgriculture. */
@Mod(NeroAgricultureCommon.MOD_ID)
public final class NeroAgricultureForge {

    public NeroAgricultureForge(FMLJavaModLoadingContext context) {
        NeroAgricultureCommon.LOGGER.info("[NeroAgriculture] Forge bootstrap");
        NeroAgricultureCommon.init();
        // Anonymous, NeroAgriculture-only crash reporting (opt-out via config/neroagriculture.properties).
        NeroAgricultureTelemetry.init();
        za.co.neroland.neroagriculture.platform.ForgeFluidFactory.registerFluidTypes(context.getModBusGroup());
        RegistrationProvider.attach(context.getModBusGroup());
        ForgeNetwork.register();
        ForgeCapabilities.register();
        if (FMLEnvironment.dist == Dist.CLIENT) ForgeClientSetup.init(context.getModBusGroup());
        RegisterCommandsEvent.BUS.addListener(event -> AgricultureCommands.register(event.getDispatcher()));
        OnDatapackSyncEvent.BUS.addListener(event -> {
            if (event.getPlayer() == null) CatalogSync.reloadAndSync(event.getPlayerList().getServer());
            else CatalogSync.syncTo(event.getPlayer());
        });
        // Optional Nerospace planet-visit adapter (join backfill + live dimension-change tracking).
        PlayerEvent.PlayerLoggedInEvent.BUS.addListener(event -> {
            if (event.getEntity() instanceof ServerPlayer player) CompatContracts.playerJoined(player);
        });
        PlayerEvent.PlayerChangedDimensionEvent.BUS.addListener(event -> {
            if (event.getEntity() instanceof ServerPlayer player) CompatContracts.playerChangedDimension(player);
        });
        // Clear the common server-scoped static caches so nothing leaks into the next (single-player) world.
        ServerStoppedEvent.BUS.addListener(event -> ServerStateReset.serverStopped());
    }
}
