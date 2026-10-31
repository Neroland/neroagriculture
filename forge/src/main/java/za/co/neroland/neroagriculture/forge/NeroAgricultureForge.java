package za.co.neroland.neroagriculture.forge;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
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
        // Forge before 1.21.6 runs EventBus 6: one mod event bus, and game events go through MinecraftForge.EVENT_BUS.
        //? if >=1.21.6 {
        net.minecraftforge.eventbus.api.bus.BusGroup modBus = context.getModBusGroup();
        //?} else {
        /*net.minecraftforge.eventbus.api.IEventBus modBus = context.getModEventBus();
        *///?}
        za.co.neroland.neroagriculture.platform.ForgeFluidFactory.registerFluidTypes(modBus);
        RegistrationProvider.attach(modBus);
        ForgeNetwork.register();
        ForgeCapabilities.register();
        if (FMLEnvironment.dist == Dist.CLIENT) ForgeClientSetup.init(modBus);
        //? if >=1.21.6 {
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
        // Record the running server (the NeroLink module resolves the world from it) and clear the common
        // server-scoped static caches so nothing leaks into the next (single-player) world.
        ServerStartedEvent.BUS.addListener(event -> ServerStateReset.serverStarted(event.getServer()));
        ServerStoppedEvent.BUS.addListener(event -> ServerStateReset.serverStopped());
        //?} else {
        /*net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent event) ->
                AgricultureCommands.register(event.getDispatcher()));
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((OnDatapackSyncEvent event) -> {
            if (event.getPlayer() == null) CatalogSync.reloadAndSync(event.getPlayerList().getServer());
            else CatalogSync.syncTo(event.getPlayer());
        });
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) CompatContracts.playerJoined(player);
        });
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) CompatContracts.playerChangedDimension(player);
        });
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent event) ->
                ServerStateReset.serverStarted(event.getServer()));
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent event) ->
                ServerStateReset.serverStopped());
        *///?}
        // Ages the crop-tower output window so neroagriculture:tower_output falls back on time.
        //? if >=1.21.9 {
        net.minecraftforge.event.TickEvent.ServerTickEvent.Post.BUS.addListener(event ->
                za.co.neroland.neroagriculture.tower.TowerOutputChannel.serverTick(event.server()));
        //?}
        //? if >=1.21.6 && <1.21.9 {
        /*net.minecraftforge.event.TickEvent.ServerTickEvent.Post.BUS.addListener(event ->
                za.co.neroland.neroagriculture.tower.TowerOutputChannel.serverTick(event.getServer()));
        *///?}
        //? if <1.21.6 {
        /*net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(
                (net.minecraftforge.event.TickEvent.ServerTickEvent.Post event) ->
                        za.co.neroland.neroagriculture.tower.TowerOutputChannel.serverTick(event.getServer()));
        *///?}
    }
}
